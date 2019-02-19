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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.MoeUserProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.codebase.WriterFactory;
import com.google.devtools.moe.client.codebase.expressions.Expression;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher.Result;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.config.MigrationConfig;
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.config.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.translation.editors.Editor;
import com.google.devtools.moe.client.translation.pipeline.TranslationPath;
import com.google.devtools.moe.client.translation.pipeline.TranslationPipeline;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.kohsuke.args4j.Option;

/**
 * Perform a one-directional merge from a branch (and specified branch point) onto the head
 * of the target repository.
 *
 * <p>This is effectively works like the {@link MagicDirective} except that it only passes in one
 * direction, and does no bookkeeping.
 */
public class MigrateBranchDirective extends Directive {
  @Option(name = "--db", required = false, usage = "Location of MOE database")
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

  private final ProjectConfig config;
  private final ProjectContext context;
  private final Repositories repositories;
  private final Ui ui;
  private final Migrator migrator;
  private final WriterFactory writerFactory;
  private final ExpressionEngine expressionEngine;

  File resultDirectory;

  @Inject
  MigrateBranchDirective(
      ProjectConfig config,
      ProjectContext context,
      Repositories repositories,
      Ui ui,
      Migrator migrator,
      WriterFactory writerFactory,
      ExpressionEngine expressionEngine) {
    this.config = config;
    this.context = context;
    this.repositories = repositories;
    this.ui = ui;
    this.migrator = migrator;
    this.writerFactory = writerFactory;
    this.expressionEngine = expressionEngine;
  }

  @Override
  protected int performDirectiveBehavior() {
    return performBranchMigration(registeredFromRepository, branchLabel, overrideUrl);
  }

  protected int performBranchMigration(
      String originalFromRepositoryName,
      String branchLabel,
      String overrideUrl) {
    return performBranchMigration(
        originalFromRepositoryName + (overrideUrl.isEmpty() ? "" : "_fork"),
        originalFromRepositoryName,
        branchLabel,
        overrideUrl);
  }

  protected int performBranchMigration(
      String fromRepositoryName,
      String originalFromRepositoryName,
      String branchLabel,
      String overrideUrl) {
    MigrationConfig migrationConfig =
        findMigrationConfigForRepository(
            overrideUrl, fromRepositoryName, originalFromRepositoryName);

    String draftRevisionLocation = "No draft revision created";
    try (Task migrationTask =
        ui.newTask(
            "perform_migration",
            "Performing migration '%s' from branch '%s'",
            migrationConfig.getName(),
            branchLabel)) {

      RepositoryConfig baseRepoConfig = config.getRepositoryConfig(originalFromRepositoryName);
      RepositoryType baseRepoType = repositories.create(originalFromRepositoryName, baseRepoConfig);
      RepositoryConfig fromRepoConfig =
          config
              .getRepositoryConfig(originalFromRepositoryName)
              .copyWithBranch(branchLabel)
              .copyWithUrl(overrideUrl);
      RepositoryType fromRepoType =
          repositories.create(migrationConfig.getFromRepository(), fromRepoConfig);

      List<Migration> migrations =
          findMigrationsBetweenBranches(
              migrationConfig.getFromRepository(),
              migrationConfig.getToRepository(),
              baseRepoType.revisionHistory(),
              fromRepoType.revisionHistory(),
              !migrationConfig.getSeparateRevisions());

      if (migrations.isEmpty()) {
        ui.message("No pending revisions to migrate in branch '%s' (as of %s)", branchLabel, "");
        migrationTask.result().append("No migrations to process");
        return 0; // autoclosed.
      }

      RepositoryExpression toRepoExp = new RepositoryExpression(migrationConfig.getToRepository());
      Writer toWriter;
      try {
        toWriter = writerFactory.createWriter(toRepoExp, context);
      } catch (WritingError e) {
        throw new MoeProblem(e, "Couldn't create local repo %s: %s", toRepoExp, e.getMessage());
      }

      for (Migration migration : migrations) {
        // For each migration, the reference to-codebase for inverse translation is the Writer,
        // since it contains the latest changes (i.e. previous migrations) to the to-repository.
        Expression referenceTargetCodebase =
            new RepositoryExpression(migrationConfig.getToRepository())
                .withOption("localroot", toWriter.getRoot().getAbsolutePath());

        try (Task performMigration =
            ui.newTask(
                "perform_individual_migration",
                "Performing individual migration '%s'",
                migration)) {

          Revision mostRecentFromRev =
              migration.fromRevisions().get(migration.fromRevisions().size() - 1);
          Codebase fromCodebase;
          try {
            String toProjectSpace =
                config.getRepositoryConfig(migration.toRepository()).getProjectSpace();
            Expression fromExpression =
                new RepositoryExpression(migration.fromRepository())
                    .atRevision(mostRecentFromRev.revId())
                    .translateTo(toProjectSpace)
                    .withReferenceTargetCodebase(referenceTargetCodebase);
            fromCodebase =
                expressionEngine.createCodebase(
                    fromExpression,
                    contextWithForkedRepository(
                        context, migrationConfig.getFromRepository(), fromRepoType));

          } catch (CodebaseCreationError e) {
            throw new MoeProblem("%s", e.getMessage());
          }
          ScrubberConfig scrubber =
              config.findScrubberConfig(originalFromRepositoryName, migration.toRepository());

          DraftRevision dr =
              migrator.migrate(
                  migration,
                  fromRepoType,
                  fromCodebase,
                  mostRecentFromRev,
                  migrationConfig.getMetadataScrubberConfig(),
                  scrubber,
                  toWriter);
          draftRevisionLocation = dr.getLocation();

          resultDirectory = toWriter.getRoot();
          performMigration.keep(toWriter); // promote this writer up.
        }
      }
      toWriter.printPushMessage(ui);
      migrationTask.keep(toWriter);
    }
    ui.message(
        "Created Draft workspace:\n%s in repository '%s'",
        draftRevisionLocation, migrationConfig.getToRepository());
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
          public Map<TranslationPath, TranslationPipeline> translators() {
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
        context
            .migrationConfigs()
            .values()
            .stream()
            .filter(input -> input.getFromRepository().equals(originalFromRepository))
            .collect(toImmutableList());
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

  private List<Migration> findMigrationsBetweenBranches(
      String fromRepoName,
      String toRepoName,
      RevisionHistory baseRevisions,
      RevisionHistory fromRevisions,
      boolean batchChanges) {

    List<Revision> toMigrate;
    try (Task t = ui.newTask("determine migrations", "Determine migrations")) {
      toMigrate = findRevisionsToMigrate(ui, fromRevisions, baseRevisions);
    }

    Result equivMatch = migrator.matchEquivalences(baseRevisions, toRepoName);

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
  static List<Revision> findRevisionsToMigrate(
      Ui ui, RevisionHistory branch, RevisionHistory parentBranch) {
    Set<String> commitsInParentBranch = new LinkedHashSet<>();
    final String repositoryName = branch.findHighestRevision(null).repositoryName();
    Revision head = parentBranch.findHighestRevision(null);
    String parentRepositoryName = head.repositoryName();

    Deque<Revision> revisionsToProcess = new ArrayDeque<>();
    try (Task ancestorTask =
        ui.newTask("scan_ancestor_branch", "Gathering revisions to consider migrating")) {
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
          revisionsToProcess.addAll(metadata.parents());
          count++;
        }
      }
      ancestorTask.result().append("Scanned revisions: " + count);
    }

    LinkedHashSet<String> commitsNotInDestinationBranch = new LinkedHashSet<>();
    try (Task t = ui.newTask("scan_target_branch", "Finding mergeable commits")) {
      revisionsToProcess = new ArrayDeque<>();
      revisionsToProcess.add(branch.findHighestRevision(null)); // most recent.

      // Walk up the branch history until we get to a commit that is already in common.
      while (!revisionsToProcess.isEmpty()) {
        Revision revision = revisionsToProcess.remove();
        RevisionMetadata metadata = branch.getMetadata(revision);
        if (metadata == null) {
          throw new MoeProblem(
              "Revision %s did not appear in branch history as expected", revision);
        }
        if (!commitsInParentBranch.contains(revision.revId())) {
          commitsNotInDestinationBranch.add(revision.revId());
          revisionsToProcess.addAll(metadata.parents());
        }
      }
    }

    return commitsNotInDestinationBranch
        .stream()
        .map(revId -> Revision.create(revId, repositoryName))
        .collect(toImmutableList())
        .reverse();
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<MigrateBranchDirective> {
    private static final String COMMAND = "migrate_branch";

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public Directive directive(MigrateBranchDirective directive) {
      return directive;
    }

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public String description() {
      return "Perform a one-directional merge from a branch onto a target repository.";
    }
  }
}
