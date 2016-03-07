/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.directives;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.MoeUserProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher.Result;
import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.parser.Expression;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.project.ScrubberConfig;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

/**
 * Perform a one-directional merge from a branch (and specified branch point) onto the head
 * of the target repository.
 *
 * <p>This is effectively works like the {@link MagicDirective} except that it only passes in one
 * direction, and does no bookkeeping.
 */
public class MigrateBranchDirective extends Directive {
  @Option(name = "--db", required = true, usage = "Location of MOE database")
  private String dbLocation = "";

  // TODO(cgruber) determine this from implicit signals.
  @Option(name = "--from_repository", required = true, usage = "The label of the source repository")
  private String registeredFromRepository = "";

  @Option(name = "--branch", required = true, usage = "the symbolic name of the imported branch")
  private String branchLabel = "";

  @Option(
    name = "--override_repository_url",
    required = false,
    usage = "the repository url to use in the case that the branch is in a fork of the repository"
  )
  private String overrideUrl = "";

  private final Lazy<ProjectConfig> config;
  private final Lazy<ProjectContext> context;
  private final Db.Factory dbFactory;
  private final Repositories repositories;
  private final Ui ui;
  private final Migrator migrator;

  File resultDirectory;

  @Inject
  MigrateBranchDirective(
      Lazy<ProjectConfig> config,
      Lazy<ProjectContext> context,
      Db.Factory dbFactory,
      Repositories repositories,
      Ui ui,
      Migrator migrator) {
    this.config = config;
    this.context = context;
    this.dbFactory = dbFactory;
    this.repositories = repositories;
    this.ui = ui;
    this.migrator = migrator;
  }

  @Override
  protected int performDirectiveBehavior() {
    return performBranchMigration(dbLocation, registeredFromRepository, branchLabel, overrideUrl);
  }

  protected int performBranchMigration(
      String dbLocation,
      String originalFromRepositoryName,
      String branchLabel,
      String overrideUrl) {
    return performBranchMigration(
        dbLocation,
        originalFromRepositoryName + (overrideUrl.isEmpty() ? "" : "_fork"),
        originalFromRepositoryName,
        branchLabel,
        overrideUrl);
  }

  protected int performBranchMigration(
      String dbLocation,
      String fromRepositoryName,
      String originalFromRepositoryName,
      String branchLabel,
      String overrideUrl) {
    Db db = dbFactory.load(dbLocation);

    MigrationConfig migrationConfig =
        findMigrationConfigForRepository(
            overrideUrl, fromRepositoryName, originalFromRepositoryName);

    Ui.Task migrationTask =
        ui.pushTask(
            "perform_migration",
            "Performing migration '%s' from branch '%s'",
            migrationConfig.getName(),
            branchLabel);


    RepositoryConfig baseRepoConfig = config.get().getRepositoryConfig(originalFromRepositoryName);
    RepositoryType baseRepoType = repositories.create(originalFromRepositoryName, baseRepoConfig);
    RepositoryConfig fromRepoConfig =
        config
            .get()
            .getRepositoryConfig(originalFromRepositoryName)
            .copyWithBranch(branchLabel)
            .copyWithUrl(overrideUrl);
    RepositoryType fromRepoType =
        repositories.create(migrationConfig.getFromRepository(), fromRepoConfig);

    List<Migration> migrations =
        findMigrationsBetweenBranches(
            db,
            migrationConfig.getFromRepository(),
            migrationConfig.getToRepository(),
            baseRepoType.revisionHistory(),
            fromRepoType.revisionHistory(),
            !migrationConfig.getSeparateRevisions());

    if (migrations.isEmpty()) {
      ui.message("No pending revisions to migrate in branch '%s' (as of %s)", branchLabel, "");
      ui.popTask(migrationTask, "No migrations to process");
      return 0;
    }

    RepositoryExpression toRepoExp = new RepositoryExpression(migrationConfig.getToRepository());
    Writer toWriter;
    try {
      toWriter = toRepoExp.createWriter(context.get());
    } catch (WritingError e) {
      throw new MoeProblem(e, "Couldn't create local repo %s: %s", toRepoExp, e.getMessage());
    }

    DraftRevision dr = null; // Store one draft revision to obtain workspace location for UI.
    for (Migration migration : migrations) {
      // For each migration, the reference to-codebase for inverse translation is the Writer,
      // since it contains the latest changes (i.e. previous migrations) to the to-repository.
      Expression referenceToCodebase =
          new RepositoryExpression(migrationConfig.getToRepository())
              .withOption("localroot", toWriter.getRoot().getAbsolutePath());

      Ui.Task performMigration =
          ui.pushTask(
              "perform_individual_migration",
              "Performing individual migration '%s'",
              migration.toString());

      Revision mostRecentFromRev =
          migration.fromRevisions().get(migration.fromRevisions().size() - 1);
      Codebase fromCodebase;
      try {
        String toProjectSpace =
            config.get().getRepositoryConfig(migration.toRepository()).getProjectSpace();

        fromCodebase =
            new RepositoryExpression(migration.fromRepository())
                .atRevision(mostRecentFromRev.revId())
                .translateTo(toProjectSpace)
                .withReferenceToCodebase(referenceToCodebase)
                .createCodebase(
                    contextWithForkedRepository(
                        context.get(), migrationConfig.getFromRepository(), fromRepoType));

      } catch (CodebaseCreationError e) {
        throw new MoeProblem(e.getMessage());
      }
      ScrubberConfig scrubber =
          config.get().findScrubberConfig(originalFromRepositoryName, migration.toRepository());

      dr =
          migrator.migrate(
              migration,
              fromRepoType,
              fromCodebase,
              mostRecentFromRev,
              migrationConfig.getMetadataScrubberConfig(),
              scrubber,
              toWriter,
              referenceToCodebase);

      resultDirectory = toWriter.getRoot();
      ui.popTaskAndPersist(performMigration, toWriter.getRoot()); // preserve toWriter
    }
    toWriter.printPushMessage();
    ui.popTaskAndPersist(migrationTask, toWriter.getRoot());
    ui.message(
        "Created Draft workspace:\n%s in repository '%s'",
        dr.getLocation(),
        toRepoExp.getRepositoryName());
    return 0;
  }

  /** Hacks up the service lookup object to insert the forked repository, if needed. */
  private static ProjectContext contextWithForkedRepository(
      final ProjectContext context, final String name, final RepositoryType repoType) {

    return context.repositories().containsKey(name)
        ? context
        : new ProjectContext() {
          @Override
          public ProjectConfig config() {
            return context.config();
          }

          @Override
          public Map<String, RepositoryType> repositories() {
            return ImmutableMap.<String, RepositoryType>builder()
                .putAll(context.repositories())
                .put(name, repoType)
                .build();
          }

          @Override
          public Map<String, Editor> editors() {
            return context.editors();
          }

          @Override
          public Map<TranslatorPath, Translator> translators() {
            return context.translators();
          }

          @Override
          public Map<String, MigrationConfig> migrationConfigs() {
            return context.migrationConfigs();
          }
        };
  }

  /**
   * Looks up the migration config using the configured from repository, returning either
   * that config as-is, or a modified version targetting a repo fork.
   */
  private MigrationConfig findMigrationConfigForRepository(
      String overrideUrl, final String fromRepository, final String originalFromRepository) {

    List<MigrationConfig> configs =
        FluentIterable.from(context.get().migrationConfigs().values())
            .filter(
                new Predicate<MigrationConfig>() {
                  @Override
                  public boolean apply(MigrationConfig input) {
                    return input.getFromRepository().equals(originalFromRepository);
                  }
                })
            .toList();
    switch (configs.size()) {
      case 0:
        throw new MoeUserProblem() {
          @Override
          public void reportTo(Ui ui) {
            ui.message(
                "No migration configurations could be found from repository '%s'", fromRepository);
          }
        };
      case 1:
        MigrationConfig migrationConfig = Iterables.getOnlyElement(configs);
        return Strings.isNullOrEmpty(overrideUrl)
            ? migrationConfig
            : migrationConfig.copyWithFromRepository(fromRepository);
      default:
        // TODO(cgruber) Allow specification of a migration if there are more than one.
        throw new MoeUserProblem() {
          @Override
          public void reportTo(Ui ui) {
            ui.message(
                "More than one migration configuration from repository '%s'", fromRepository);
          }
        };
    }
  }

  @Override
  public String getDescription() {
    return "Updates database and performs all migrations";
  }

  private List<Migration> findMigrationsBetweenBranches(
      Db db,
      String fromRepoName,
      String toRepoName,
      RevisionHistory baseRevisions,
      RevisionHistory fromRevisions,
      boolean batchChanges) {
    Ui.Task determineMigrationsTask = ui.pushTask("determind migrations", "Determine migrations");
    List<Revision> toMigrate = findDescendantRevisions(fromRevisions, baseRevisions);
    ui.popTask(determineMigrationsTask, "");

    Result equivMatch = migrator.matchEquivalences(db, baseRevisions, toRepoName);

    RepositoryEquivalence latestEquivalence = equivMatch.getEquivalences().get(0);

    ui.message(
        "Migrating %d revisions in %s (branch %s): %s",
        toMigrate.size(),
        fromRepoName,
        branchLabel,
        Joiner.on(", ").join(toMigrate));
    if (!batchChanges) {
      ImmutableList.Builder<Migration> migrations = ImmutableList.builder();
      for (Revision fromRev : toMigrate) {
        migrations.add(
            Migration.create(
                "custom_branch_import",
                fromRepoName,
                toRepoName,
                ImmutableList.of(fromRev),
                latestEquivalence));
      }
      return migrations.build();
    } else {
      return ImmutableList.of(
          Migration.create(
              "custom_branch_import", fromRepoName, toRepoName, toMigrate, latestEquivalence));
    }
  }

  /**
   * Returns a list of commits that are ancestors of the {@code branch} HEAD, which are not also
   * found in {@parentBranch}.
   */
  /*
   * TODO(cgruber): Profile costs.
   * TODO(cgruber): Optimize the loops and metadata fetching.
   * TODO(cgruber): Look at the feasibility of a more incremental approach based on profiling.
   *
   * This code, depending on the repository implementation, can be expsensive, and result in
   * a lot of trips to the filesystem or executions of external commands.  Some of the looping
   * and metadata fetching should be exposed in the RepositoryType where batch commands can
   * be implemented more efficiently.
   *
   * This is particularly vulnerable to large depots, but our largest depots are <2000 commits, so
   * there is a window.
   */
  @VisibleForTesting
  List<Revision> findDescendantRevisions(RevisionHistory branch, RevisionHistory parentBranch) {
    Set<String> commitsInParentBranch = new LinkedHashSet<>();
    final String repositoryName = branch.findHighestRevision(null).repositoryName();
    Revision head = parentBranch.findHighestRevision(null);
    String parentRepositoryName = head.repositoryName();

    Ui.Task ancestorTask = ui.pushTask("scan_ancestor_branch", "Gathering ancestor revisions");
    Deque<Revision> revisionsToProcess = new ArrayDeque<>();
    revisionsToProcess.add(head);
    int count = 0;
    while (!revisionsToProcess.isEmpty()) {
      Revision revision = revisionsToProcess.remove();
      if (!commitsInParentBranch.contains(revision.revId())) {
        commitsInParentBranch.add(revision.revId());
        RevisionMetadata metadata =
            parentBranch.getMetadata(Revision.create(revision.revId(), parentRepositoryName));
        if (metadata == null) {
          throw new MoeProblem("Could not load revision metadata for %s", revision);
        }
        revisionsToProcess.addAll(metadata.parents);
        count++;
      }
    }
    ui.popTask(ancestorTask, "Scanned revisions: " + count);

    Ui.Task migrationBranchTask = ui.pushTask("scan_target_branch", "Finding mergeable commits");
    LinkedHashSet<String> commitsNotInParentBranch = new LinkedHashSet<>();
    revisionsToProcess = new ArrayDeque<>();
    revisionsToProcess.add(branch.findHighestRevision(null));
    while (!revisionsToProcess.isEmpty()) {
      Revision revision = revisionsToProcess.remove();
      RevisionMetadata metadata = branch.getMetadata(revision);
      if (metadata == null) {
        throw new MoeProblem("Revision %s did not appear in branch history as expected", revision);
      }
      if (!commitsInParentBranch.contains(revision.revId())) {
        commitsNotInParentBranch.add(revision.revId());
        revisionsToProcess.addAll(metadata.parents);
      }
    }
    ui.popTask(migrationBranchTask, "");

    return FluentIterable.from(commitsNotInParentBranch)
        .transform(
            new Function<String, Revision>() {
              @Override
              public Revision apply(String revId) {
                return Revision.create(revId, repositoryName);
              }
            })
        .toList()
        .reverse();
  }
}
