// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Bookkeeper;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Perform the necessary checks to update MOE's db.
 *
 */
public class BookkeepingDirective extends Directive {
  @Option(name = "--db", required = true, usage = "Location of MOE database")
  String dbLocation = "";

  private final Ui ui;
  private final Bookkeeper bookkeeper;

  @Inject
  BookkeepingDirective(ProjectContextFactory contextFactory, Ui ui, Bookkeeper bookkeeper) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.ui = ui;
    this.bookkeeper = bookkeeper;
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
    return bookkeeper.bookkeep(db, dbLocation, context());
  }

  @Override
  public String getDescription() {
    return "Updates the database";
  }
}
