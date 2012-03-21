// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.RevisionsSinceEquivalenceLogic;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.List;

/**
 * Get all Revisions since last Equivalence
 *
 */
public class RevisionsSinceEquivalenceDirective implements Directive {

  private final RevisionsSinceEquivalenceOptions options = new RevisionsSinceEquivalenceOptions();

  @Override
  public RevisionsSinceEquivalenceOptions getFlags() {
    return options;
  }
  public RevisionsSinceEquivalenceDirective() {}

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
      db = new DummyDb(false);
    } else {
      // TODO(user): also allow for url dbLocation types
      try {
        db = FileDb.makeDbFromFile(options.dbLocation);
      } catch (MoeProblem e) {
        AppContext.RUN.ui.error(e, "Couldn't create DB");
        return 1;
      }
    }

    List<Revision> revisionsSinceEquivalence =
        RevisionsSinceEquivalenceLogic.getRevisionsSinceEquivalence(
            options.fromRepository, options.toRepository, db, context);

    AppContext.RUN.ui.info(
        "Found " + revisionsSinceEquivalence.size() + " revisions since equivalence.");
    for (Revision rev : revisionsSinceEquivalence) {
      AppContext.RUN.ui.info("  Revision: " + rev);
    }
    return 0;
  }

  static class RevisionsSinceEquivalenceOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--db", required = true,
            usage = "Location of MOE database")
    String dbLocation = "";
    @Option(name = "--from_repository", required = true,
            usage = "Revision expression describing repository to find Revisions in")
    String fromRepository = "";
    @Option(name = "--to_repository", required = true,
            usage = "Name of Repository to check for Equivalences in")
    String toRepository = "";
  }
}
