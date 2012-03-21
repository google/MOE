// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.LastEquivalenceLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

/**
 * Get the last Equivalence between two repositories.
 *
 */
public class LastEquivalenceDirective implements Directive {

  private final LastEquivalenceOptions options = new LastEquivalenceOptions();

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
        AppContext.RUN.ui.error(e, "Couldn't create DB");
        return 1;
      }
    }

    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(options.fromRepository);
    } catch (ParseError e) {
      AppContext.RUN.ui.error(e, "Couldn't parse " + options.fromRepository);
      return 1;
    }

    Repository r = context.repositories.get(repoEx.getRepositoryName());
    if (r == null) {
      AppContext.RUN.ui.error("No repository" + repoEx.getRepositoryName() + ".");
      return 1;
    }

    RevisionHistory rh = r.revisionHistory;
    if (rh == null) {
      AppContext.RUN.ui.error("Repository " + r.name + " does not support revision history.");
      return 1;
    }

    Revision rev = rh.findHighestRevision(repoEx.getOption("revision"));

    Equivalence lastEquiv = LastEquivalenceLogic.lastEquivalence(
        options.withRepository, rev, db, rh);

    if (lastEquiv == null) {
      AppContext.RUN.ui.info(
          String.format("No equivalence was found between %s and %s starting from %s.",
              rev.repositoryName, options.withRepository, rev));
    } else {
      AppContext.RUN.ui.info(String.format("Last equivalence: %s", lastEquiv.toString()));
    }

    return 0;
  }

  static class LastEquivalenceOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--db", required = true,
            usage = "Location of MOE database")
    String dbLocation = "";
    @Option(name = "--from_repository", required = true,
        usage = "Expression for the from-repository to check for Equivalences in")
    String fromRepository = "";
    @Option(name = "--with_repository", required = true,
            usage = "Name of the to-repository to check for Equivalences in")
    String withRepository = "";
  }

}
