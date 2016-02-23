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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.database.Bookkeeper;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.parser.Expression;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ScrubberConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Update the MOE db then perform all migration(s) specified in the MOE config. Repeated
 * invocations, then, will result in a state of all pending migrations performed, and all performed
 * migrations and new equivalences stored in the db.
 */
public class MagicDirective extends Directive {
  @Option(name = "--db", required = true, usage = "Location of MOE database")
  String dbLocation = "";

  @Option(
    name = "--migration",
    required = false,
    usage = "Migrations to perform; can include multiple --migration options"
  )
  List<String> migrations = Lists.newArrayList();

  @Option(
    name = "--skip_revision",
    required = false,
    usage = "Revisions to skip; can include multiple --skip_revision options"
  )
  List<String> skipRevisions = new ArrayList<>();

  private final Lazy<ProjectConfig> config;
  private final Lazy<ProjectContext> context;
  private final Db.Factory dbFactory;
  private final Ui ui;
  private final Migrator migrator;
  private final Bookkeeper bookkeeper;

  @Inject
  MagicDirective(
      Lazy<ProjectConfig> config,
      Lazy<ProjectContext> context,
      Db.Factory dbFactory,
      Ui ui,
      Bookkeeper bookkeeper,
      Migrator migrator) {
    this.context = context;
    this.config = config;
    this.dbFactory = dbFactory;
    this.ui = ui;
    this.bookkeeper = bookkeeper;
    this.migrator = migrator;
  }

  @Override
  protected int performDirectiveBehavior() {
    Db db = dbFactory.load(dbLocation);

    List<String> migrationNames =
        ImmutableList.copyOf(
            migrations.isEmpty() ? context.get().migrationConfigs().keySet() : migrations);

    Set<String> skipRevisions = ImmutableSet.copyOf(this.skipRevisions);

    if (bookkeeper.bookkeep(db, context.get()) != 0) {
      // Bookkeeping has failed, so fail here as well.
      return 1;
    }

    ImmutableList.Builder<String> migrationsMadeBuilder = ImmutableList.builder();

    for (String migrationName : migrationNames) {
      Ui.Task migrationTask =
          ui.pushTask("perform_migration", "Performing migration '%s'", migrationName);

      MigrationConfig migrationConfig = context.get().migrationConfigs().get(migrationName);
      if (migrationConfig == null) {
        ui.message("No migration found with name %s... skipping.", migrationName);
        continue;
      }

      RepositoryType fromRepo = context.get().getRepository(migrationConfig.getFromRepository());
      List<Migration> migrations =
          migrator.findMigrationsFromEquivalency(fromRepo, migrationConfig, db);

      if (migrations.isEmpty()) {
        ui.message("No pending revisions to migrate for %s", migrationName);
        continue;
      }

      RepositoryEquivalence lastEq = migrations.get(0).sinceEquivalence();
      // toRe represents toRepo at the revision of last equivalence with fromRepo.
      RepositoryExpression toRe = new RepositoryExpression(migrationConfig.getToRepository());
      if (lastEq != null) {
        toRe =
            toRe.atRevision(
                lastEq.getRevisionForRepository(migrationConfig.getToRepository()).revId());
      }

      Writer toWriter;
      try {
        toWriter = toRe.createWriter(context.get());
      } catch (WritingError e) {
        throw new MoeProblem("Couldn't create local repo %s: %s", toRe, e);
      }

      DraftRevision dr = null;
      int currentlyPerformedMigration = 1; // To display to users.
      for (Migration migration : migrations) {

        // First check if we should even do this migration at all.
        int skipped = 0;
        for (Revision revision : migration.fromRevisions()) {
          if (skipRevisions.contains(revision.toString())) {
            skipped++;
          }
        }
        if (skipped > 0) {
          if (skipped != migration.fromRevisions().size()) {
            throw new MoeProblem(
                "Cannot skip subset of revisions in a single migration: " + migration);
          }
          ui.message(
              "Skipping %s/%s migration `%s`",
              currentlyPerformedMigration++,
              migrations.size(),
              migration);
          continue;
        }

        // For each migration, the reference to-codebase for inverse translation is the Writer,
        // since it contains the latest changes (i.e. previous migrations) to the to-repository.
        Expression referenceToCodebase =
            new RepositoryExpression(migrationConfig.getToRepository())
                .withOption("localroot", toWriter.getRoot().getAbsolutePath());

        Ui.Task oneMigrationTask =
            ui.pushTask(
                "perform_individual_migration",
                "Performing %s/%s migration '%s'",
                currentlyPerformedMigration++,
                migrations.size(),
                migration);

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
                  .createCodebase(context.get());

        } catch (CodebaseCreationError e) {
          throw new MoeProblem(e.getMessage());
        }

        RepositoryType fromRepoType =
            context.get().getRepository(migrationConfig.getFromRepository());
        ScrubberConfig scrubber =
            config.get().findScrubberConfig(migration.fromRepository(), migration.toRepository());
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

        ui.popTask(oneMigrationTask, "");
      }

      // TODO(user): Add properly formatted one-DraftRevison-per-Migration message for svn.
      migrationsMadeBuilder.add(
          String.format(
              "%s in repository %s", dr.getLocation(), migrationConfig.getToRepository()));
      toWriter.printPushMessage();
      ui.popTaskAndPersist(migrationTask, toWriter.getRoot());
    }

    List<String> migrationsMade = migrationsMadeBuilder.build();
    if (migrationsMade.isEmpty()) {
      ui.message("No migrations made.");
    } else {
      ui.message("Created Draft Revisions:\n" + Joiner.on("\n").join(migrationsMade));
    }

    return 0;
  }

  @Override
  public String getDescription() {
    return "Updates database and performs all migrations";
  }
}
