// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.database.Bookkeeper;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.project.ProjectContextFactory;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Perform the necessary checks to update MOE's db.
 *
 */
public class BookkeepingDirective extends Directive {
  @Option(name = "--db", required = true, usage = "Location of MOE database")
  String dbLocation = "";

  private final Db.Factory dbFactory;
  private final Bookkeeper bookkeeper;

  @Inject
  BookkeepingDirective(
      ProjectContextFactory contextFactory, Db.Factory dbFactory, Bookkeeper bookkeeper) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.dbFactory = dbFactory;
    this.bookkeeper = bookkeeper;
  }

  @Override
  protected int performDirectiveBehavior() {
    Db db = dbFactory.load(dbLocation);
    return bookkeeper.bookkeep(db, context());
  }

  @Override
  public String getDescription() {
    return "Updates the database";
  }
}
