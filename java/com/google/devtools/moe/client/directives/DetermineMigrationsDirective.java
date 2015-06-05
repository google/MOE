// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.DetermineMigrationsLogic;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Print the results of {@link DetermineMigrationsLogic}.
 *
 */
public class DetermineMigrationsDirective extends Directive {
  private final DetermineMigrationsOptions options = new DetermineMigrationsOptions();

  private final ProjectContextFactory contextFactory;
  private final Ui ui;

  @Inject
  public DetermineMigrationsDirective(ProjectContextFactory contextFactory, Ui ui) {
    this.contextFactory = contextFactory;
    this.ui = ui;
  }

  @Override
  public MoeOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = contextFactory.create(options.configFilename);
    } catch (InvalidProject e) {
      ui.error(e, "Error creating project");
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
        ui.error(e, "Error creating DB");
        return 1;
      }
    }

    MigrationConfig config = context.migrationConfigs.get(options.migrationName);
    if (config == null) {
      ui.error("No migration found with name " + options.migrationName);
      return 1;
    }

    List<Migration> migrations = DetermineMigrationsLogic.determineMigrations(context, config, db);
    for (Migration migration : migrations) {
      ui.info("Pending migration: " + migration);
    }

    return 0;
  }

  @Override
  public String getDescription() {
    return "Finds and prints the unmigrated revisions for a migration";
  }

  static class DetermineMigrationsOptions extends MoeOptions {

    @Option(name = "--config_file", required = true, usage = "Location of MOE config file")
    String configFilename = "";

    @Option(
        name = "--migration_name",
        required = true,
        usage = "Name of migration, as found in config file")
    String migrationName = "";

    @Option(name = "--db", required = true, usage = "Location of MOE database")
    String dbLocation = "";
  }
}
