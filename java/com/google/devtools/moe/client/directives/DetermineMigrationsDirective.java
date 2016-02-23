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

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Print the results of
 * {@link Migrator#findMigrationsFromEquivalency(RepositoryType, MigrationConfig, Db)}
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

  private final Lazy<ProjectContext> context;
  private final Db.Factory dbFactory;
  private final Ui ui;
  private final Migrator migrator;

  @Inject
  public DetermineMigrationsDirective(
      Lazy<ProjectContext> context, Db.Factory dbFactory, Ui ui, Migrator migrator) {
    this.context = context;
    this.dbFactory = dbFactory;
    this.ui = ui;
    this.migrator = migrator;
  }

  @Override
  protected int performDirectiveBehavior() {
    Db db = dbFactory.load(dbLocation);

    MigrationConfig config = context.get().migrationConfigs().get(migrationName);
    if (config == null) {
      throw new MoeProblem("No migration found with name " + migrationName);
    }

    RepositoryType fromRepo = context.get().getRepository(config.getFromRepository());
    List<Migration> migrations = migrator.findMigrationsFromEquivalency(fromRepo, config, db);
    for (Migration migration : migrations) {
      ui.message("Pending migration: " + migration);
    }

    return 0;
  }

  @Override
  public String getDescription() {
    return "Finds and prints the unmigrated revisions for a migration";
  }
}
