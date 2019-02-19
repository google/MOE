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
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.codebase.WriterFactory;
import com.google.devtools.moe.client.codebase.expressions.Expression;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.database.Bookkeeper;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.config.MigrationConfig;
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.config.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.kohsuke.args4j.Option;

/**
 * Update the MOE db then perform all migration(s) specified in the MOE config. Repeated
 * invocations, then, will result in a state of all pending migrations performed, and all performed
 * migrations and new equivalences stored in the db.
 */
public class MagicDirective extends Directive {
  @Option(name = "--db", required = false, usage = "Location of MOE database")
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
    usage = "Revisions to skip, e.g. 'internal{1234}'; can include multiple --skip_revision options"
  )
  List<String> skipRevisions = new ArrayList<>();

  @Option(name = "--skip_bookkeeping", required = false, usage = "Omits bookkeeping operations")
  boolean skipBookkeeping = false;

  private final ProjectConfig config;
  private final ProjectContext context;
  private final Ui ui;
  private final Migrator migrator;
  private final Bookkeeper bookkeeper;
  private final WriterFactory writerFactory;
  private final ExpressionEngine expressionEngine;

  @Inject
  MagicDirective(
      ProjectConfig config,
      ProjectContext context,
      Ui ui,
      Bookkeeper bookkeeper,
      Migrator migrator,
      WriterFactory writerFactory,
      ExpressionEngine expressionEngine) {
    this.context = context;
    this.config = config;
    this.ui = ui;
    this.bookkeeper = bookkeeper;
    this.migrator = migrator;
    this.writerFactory = writerFactory;
    this.expressionEngine = expressionEngine;
  }

  @Override
  protected int performDirectiveBehavior() {
    List<String> migrationNames =
        ImmutableList.copyOf(
            migrations.isEmpty() ? context.migrationConfigs().keySet() : migrations);

    Set<String> skipRevisions = ImmutableSet.copyOf(this.skipRevisions);

    if (!skipBookkeeping && bookkeeper.bookkeep() != 0) {
      // Bookkeeping has failed, so fail here as well.
      return 1;
    }

    ImmutableList.Builder<String> migrationsMadeBuilder = ImmutableList.builder();

    for (String migrationName : migrationNames) {
      try (Task migrationTask =
          ui.newTask("perform_migration", "Performing migration '%s'", migrationName)) {

        MigrationConfig migrationConfig = context.migrationConfigs().get(migrationName);
        if (migrationConfig == null) {
          ui.message("No migration found with name %s... skipping.", migrationName);
          continue;
        }

        RepositoryType fromRepositoryType =
            context.getRepository(migrationConfig.getFromRepository());
        List<Migration> migrations =
            migrator.findMigrationsFromEquivalency(fromRepositoryType, migrationConfig);

        if (migrations.isEmpty()) {
          ui.message("No pending revisions to migrate for %s", migrationName);
          continue;
        }

        RepositoryEquivalence lastRecordedEquivalence = migrations.get(0).sinceEquivalence();
        RepositoryExpression targetRepositoryPointOfEquivalency =
            new RepositoryExpression(migrationConfig.getToRepository());
        if (lastRecordedEquivalence != null) {
          targetRepositoryPointOfEquivalency =
              targetRepositoryPointOfEquivalency.atRevision(
                  lastRecordedEquivalence
                      .getRevisionForRepository(migrationConfig.getToRepository())
                      .revId());
        }

        Writer targetCodebaseWriter;
        try {
          targetCodebaseWriter =
              migrationTask.keep(
                  writerFactory.createWriter(targetRepositoryPointOfEquivalency, context));
        } catch (WritingError e) {
          throw new MoeProblem(
              "Couldn't create local repo %s: %s", targetRepositoryPointOfEquivalency, e);
        }

        DraftRevision draftRevision = null;
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
                  "Cannot skip subset of revisions in a single migration: %s", migration);
            }
            ui.message(
                "Skipping %s/%s migration `%s`",
                currentlyPerformedMigration++, migrations.size(), migration);
            continue;
          }

          // For each migration, the reference to-codebase for inverse translation is the Writer,
          // since it contains the latest changes (i.e. previous migrations) to the to-repository.
          Expression referenceTargetCodebase =
              new RepositoryExpression(migrationConfig.getToRepository())
                  .withOption("localroot", targetCodebaseWriter.getRoot().getAbsolutePath());
          try (Task oneMigrationTask =
              ui.newTask(
                  "perform_individual_migration",
                  "Performing %s/%s migration '%s'",
                  currentlyPerformedMigration++,
                  migrations.size(),
                  migration)) {

            Revision mostRecentFromRev =
                migration.fromRevisions().get(migration.fromRevisions().size() - 1);
            Codebase fromCodebase;
            try {
              String targetProjectSpace =
                  config.getRepositoryConfig(migration.toRepository()).getProjectSpace();
              Expression fromExpression =
                  new RepositoryExpression(migration.fromRepository())
                      .atRevision(mostRecentFromRev.revId())
                      .translateTo(targetProjectSpace)
                      .withReferenceTargetCodebase(referenceTargetCodebase);
              fromCodebase = expressionEngine.createCodebase(fromExpression, context);

            } catch (CodebaseCreationError e) {
              throw new MoeProblem("%s", e.getMessage());
            }

            RepositoryType fromRepoType =
                context.getRepository(migrationConfig.getFromRepository());
            ScrubberConfig scrubber =
                config.findScrubberConfig(migration.fromRepository(), migration.toRepository());
            draftRevision =
                migrator.migrate(
                    migration,
                    fromRepoType,
                    fromCodebase,
                    mostRecentFromRev,
                    migrationConfig.getMetadataScrubberConfig(),
                    scrubber,
                    targetCodebaseWriter);
          }
        }

        // TODO(user): Add properly formatted one-DraftRevison-per-Migration message for svn.
        migrationsMadeBuilder.add(
            String.format(
                "%s in repository %s",
                draftRevision.getLocation(), migrationConfig.getToRepository()));
        targetCodebaseWriter.printPushMessage(ui);
      }
    }

    List<String> migrationsMade = migrationsMadeBuilder.build();
    if (migrationsMade.isEmpty()) {
      ui.message("No migrations made.");
    } else {
      ui.message("Created Draft Revisions:\n" + Joiner.on("\n").join(migrationsMade));
    }

    return 0;
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<MagicDirective> {
    private static final String COMMAND = "magic";

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public Directive directive(MagicDirective directive) {
      return directive;
    }

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public String description() {
      return "Updates database and performs any configured migrations that have pending commits";
    }
  }
}
