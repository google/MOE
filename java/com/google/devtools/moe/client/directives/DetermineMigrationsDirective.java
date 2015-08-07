// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.migrations.Migrator;
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
  @Option(
    name = "--migration_name",
    required = true,
    usage = "Name of migration, as found in config file"
  )
  String migrationName = "";

  @Option(name = "--db", required = true, usage = "Location of MOE database")
  String dbLocation = "";

  private final Ui ui;
  private final Migrator migrator;

  @Inject
  public DetermineMigrationsDirective(
      ProjectContextFactory contextFactory, Ui ui, Migrator migrator) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.ui = ui;
    this.migrator = migrator;
  }

  @Override
  protected int performDirectiveBehavior() {
    Db db;
    if (dbLocation.equals("dummy")) {
      db = new DummyDb(true);
    } else {
      // TODO(user): also allow for url dbLocation types
      try {
        db = FileDb.makeDbFromFile(dbLocation);
      } catch (MoeProblem e) {
        ui.error(e, "Error creating DB");
        return 1;
      }
    }

    MigrationConfig config = context().migrationConfigs().get(migrationName);
    if (config == null) {
      ui.error("No migration found with name " + migrationName);
      return 1;
    }

    List<Migration> migrations = migrator.determineMigrations(context(), config, db);
    for (Migration migration : migrations) {
      ui.info("Pending migration: " + migration);
    }

    return 0;
  }

  @Override
  public String getDescription() {
    return "Finds and prints the unmigrated revisions for a migration";
  }
}
