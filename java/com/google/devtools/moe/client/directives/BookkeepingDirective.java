// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.BookkeepingLogic;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Perform the necessary checks to update MOE's db.
 *
 */
public class BookkeepingDirective extends Directive {
  private final BookkeepingOptions options = new BookkeepingOptions();

  private final ProjectContextFactory contextFactory;
  private final Ui ui;

  @Inject
  BookkeepingDirective(ProjectContextFactory contextFactory, Ui ui) {
    this.contextFactory = contextFactory;
    this.ui = ui;
  }

  @Override
  public BookkeepingOptions getFlags() {
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

    List<String> names = ImmutableList.copyOf(context.migrationConfigs.keySet());
    return BookkeepingLogic.bookkeep(names, db, options.dbLocation, context);
  }

  @Override
  public String getDescription() {
    return "Updates the database";
  }

  static class BookkeepingOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--db", required = true,
            usage = "Location of MOE database")
    String dbLocation = "";
  }
}
