// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.FindEquivalenceLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Finds revisions in a repository that are equivalent to a given revision.
 *
 */
public class FindEquivalenceDirective extends Directive {
  @Option(name = "--db", required = true, usage = "Location of MOE database")
  String dbLocation = "";

  @Option(
    name = "--from_repository",
    required = true,
    usage =
        "A Repository expression to find equivalences for (in in_repository), e.g. "
            + "'internal(revision=3,4,5)'"
  )
  String fromRepository = "";

  @Option(
    name = "--in_repository",
    required = true,
    usage = "Which repository to find equivalences in"
  )
  String inRepository = "";

  private final Ui ui;

  @Inject
  FindEquivalenceDirective(ProjectContextFactory contextFactory, Ui ui) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.ui = ui;
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

    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(fromRepository);
    } catch (ParseError e) {
      ui.error(e, "Couldn't parse " + fromRepository);
      return 1;
    }

    List<Revision> revs = Revision.fromRepositoryExpression(repoEx, context());

    FindEquivalenceLogic.printEquivalences(revs, inRepository, db);
    return 0;
  }

  @Override
  public String getDescription() {
    return "Finds revisions in one repository that are equivalent to a given revision in another";
  }
}
