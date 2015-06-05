// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.base.Joiner;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.LastEquivalenceLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Get the last Equivalence between two repositories.
 *
 */
public class LastEquivalenceDirective extends Directive {
  private final LastEquivalenceOptions options = new LastEquivalenceOptions();

  private final ProjectContextFactory contextFactory;
  private final Ui ui;

  @Inject
  LastEquivalenceDirective(ProjectContextFactory contextFactory, Ui ui) {
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
        ui.error(e, "Couldn't create DB");
        return 1;
      }
    }

    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(options.fromRepository);
    } catch (ParseError e) {
      ui.error(e, "Couldn't parse " + options.fromRepository);
      return 1;
    }

    Repository r = context.getRepository(repoEx.getRepositoryName());

    RevisionHistory rh = r.revisionHistory();
    if (rh == null) {
      ui.error("Repository " + r.name() + " does not support revision history.");
      return 1;
    }

    Revision rev = rh.findHighestRevision(repoEx.getOption("revision"));

    List<Equivalence> lastEquivs =
        LastEquivalenceLogic.lastEquivalence(options.withRepository, rev, db, rh);

    if (lastEquivs.isEmpty()) {
      ui.info(
          String.format(
              "No equivalence was found between %s and %s starting from %s.",
              rev.repositoryName,
              options.withRepository,
              rev));
    } else {
      ui.info(String.format("Last equivalence: %s", Joiner.on(", ").join(lastEquivs)));
    }

    return 0;
  }

  @Override
  public String getDescription() {
    return "Finds the last equivalence";
  }

  static class LastEquivalenceOptions extends MoeOptions {

    @Option(name = "--config_file", required = true, usage = "Location of MOE config file")
    String configFilename = "";

    @Option(name = "--db", required = true, usage = "Location of MOE database")
    String dbLocation = "";

    @Option(
        name = "--from_repository",
        required = true,
        usage = "Expression for the from-repository to check for Equivalences in")
    String fromRepository = "";

    @Option(
        name = "--with_repository",
        required = true,
        usage = "Name of the to-repository to check for Equivalences in")
    String withRepository = "";
  }
}
