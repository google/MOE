// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.BookkeepingLogic;
import com.google.devtools.moe.client.logic.DetermineMigrationsLogic;
import com.google.devtools.moe.client.logic.OneMigrationLogic;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.parser.Expression;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.DummyDb;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import org.kohsuke.args4j.Option;

import java.util.List;

/**
 * Update the MOE db then perform all migration(s) specified in the MOE config. Repeated
 * invocations, then, will result in a state of all pending migrations performed, and all performed
 * migrations and new equivalences stored in the db.
 *
 */
public class MagicDirective implements Directive {

  private final MagicOptions options = new MagicOptions();

  public MagicDirective() {}

  @Override
  public MagicOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e, "Error creating project");
      return 1;
    }

    Db db;
    if (options.dbLocation.equals("dummy")) {
      db = new DummyDb(true);
    } else {
      // TODO(user): also allow for url dbLocation types
      try {
        db = FileDb.makeDbFromFile(options.dbLocation);
      } catch (MoeProblem e) {
        AppContext.RUN.ui.error(e, "Error creating DB");
        return 1;
      }
    }

    List<String> migrationNames = ImmutableList.copyOf(
        options.migrations.isEmpty() ? context.migrationConfigs.keySet() : options.migrations);

    if (BookkeepingLogic.bookkeep(migrationNames, db, options.dbLocation, context) != 0) {
      // Bookkeeping has failed, so fail here as well.
      return 1;
    }

    ImmutableList.Builder<String> migrationsMadeBuilder = ImmutableList.builder();

    for (String migrationName : migrationNames) {
      Ui.Task migrationTask = AppContext.RUN.ui.pushTask(
          "perform_migration",
          String.format("Performing migration '%s'", migrationName));

      MigrationConfig migrationConfig = context.migrationConfigs.get(migrationName);
      if (migrationConfig == null) {
        AppContext.RUN.ui.error("No migration found with name " + migrationName);
        continue;
      }

      List<Migration> migrations =
          DetermineMigrationsLogic.determineMigrations(context, migrationConfig, db);

      if (migrations.isEmpty()) {
        AppContext.RUN.ui.info("No pending revisions to migrate for " + migrationName);
        continue;
      }

      Equivalence lastEq = migrations.get(0).sinceEquivalence;
      // toRe represents toRepo at the revision of last equivalence with fromRepo.
      RepositoryExpression toRe = new RepositoryExpression(migrationConfig.getToRepository());
      if (lastEq != null) {
        toRe = toRe.atRevision(
            lastEq.getRevisionForRepository(migrationConfig.getToRepository()).revId);
      }

      Writer toWriter;
      try {
        toWriter = toRe.createWriter(context);
      } catch (WritingError e) {
        throw new MoeProblem("Couldn't create local repo " + toRe + ": " + e);
      }

      DraftRevision dr = null;
      Revision lastMigratedRevision = null;
      if (lastEq != null) {
        lastMigratedRevision = lastEq.getRevisionForRepository(migrationConfig.getFromRepository());
      }

      for (Migration m : migrations) {
        // For each migration, the reference to-codebase for inverse translation is the Writer,
        // since it contains the latest changes (i.e. previous migrations) to the to-repository.
        Expression referenceToCodebase = new RepositoryExpression(migrationConfig.getToRepository())
              .withOption("localroot", toWriter.getRoot().getAbsolutePath());

        Ui.Task oneMigrationTask = AppContext.RUN.ui.pushTask(
            "perform_individual_migration",
            String.format("Performing individual migration '%s'", m.toString()));
        dr = OneMigrationLogic.migrate(m, context, toWriter, referenceToCodebase);
        lastMigratedRevision = m.fromRevisions.get(m.fromRevisions.size() - 1);
        AppContext.RUN.ui.popTask(oneMigrationTask, "");
      }

      // TODO(user): Add properly formatted one-DraftRevison-per-Migration message for svn.
      migrationsMadeBuilder.add(String.format(
          "%s in repository %s", dr.getLocation(), migrationConfig.getToRepository()));
      toWriter.printPushMessage();
      AppContext.RUN.ui.popTaskAndPersist(migrationTask, toWriter.getRoot());
    }

    List<String> migrationsMade = migrationsMadeBuilder.build();
    if (migrationsMade.isEmpty()) {
      AppContext.RUN.ui.info("No migrations made.");
    } else {
      AppContext.RUN.ui.info("Created Draft Revisions:\n" + Joiner.on("\n").join(migrationsMade));
    }

    return 0;
  }

  static class MagicOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--db", required = true,
            usage = "Location of MOE database")
    String dbLocation = "";
    @Option(name = "--migration", required = false,
            usage = "Migrations to perform; can include multiple --migration options")
    List<String> migrations = Lists.newArrayList();
  }
}
