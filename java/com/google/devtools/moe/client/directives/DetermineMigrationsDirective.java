// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.DetermineMigrationsLogic;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.List;

/**
 * Print the results of {@link DetermineMigrationsLogic}.
 *
 */
public class DetermineMigrationsDirective implements Directive {

  private final DetermineMigrationsOptions options = new DetermineMigrationsOptions();

  @Override
  public MoeOptions getFlags() {
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

    MigrationConfig config = context.migrationConfigs.get(options.migrationName);
    if (config == null) {
      AppContext.RUN.ui.error("No migration found with name " + options.migrationName);
      return 1;
    }

    List<Migration> migrations =
        DetermineMigrationsLogic.determineMigrations(context, config, db);
    for (Migration migration : migrations) {
      AppContext.RUN.ui.info("Pending migration: " + migration);
    }

    return 0;
  }

  static class DetermineMigrationsOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--migration_name", required = true,
        usage = "Name of migration, as found in config file")
    String migrationName = "";
    @Option(name = "--db", required = true,
            usage = "Location of MOE database")
    String dbLocation = "";
  }
}
