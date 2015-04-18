// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.FindEquivalenceLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.List;

/**
 * Finds revisions in a repository that are equivalent to a given revision.
 *
 */
public class FindEquivalenceDirective implements Directive {

  private final FindEquivalenceOptions options = new FindEquivalenceOptions();

  public FindEquivalenceDirective() {}

  @Override
  public FindEquivalenceOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    Db db;
    if (options.dbLocation.equals("dummy")) {
      db = new DummyDb(true);
    } else {
      // TODO(user): also allow for url dbLocation types
      try {
        db = FileDb.makeDbFromFile(options.dbLocation);
      } catch (MoeProblem e) {
        Injector.INSTANCE.ui.error(e, "Error creating DB");
        return 1;
      }
    }

    ProjectContext context;
    try {
      context = Injector.INSTANCE.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      Injector.INSTANCE.ui.error(e, "Error creating project");
      return 1;
    }

    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(options.fromRepository);
    } catch (ParseError e) {
      Injector.INSTANCE.ui.error(e, "Couldn't parse " + options.fromRepository);
      return 1;
    }

    List<Revision> revs = Revision.fromRepositoryExpression(repoEx, context);

    FindEquivalenceLogic.printEquivalences(revs, options.inRepository, db);
    return 0;
  }

  static class FindEquivalenceOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
      usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--db", required = true,
            usage = "Location of MOE database")
    String dbLocation = "";
    @Option(name = "--from_repository", required = true,
        usage = "A Repository expression to find equivalences for (in in_repository), e.g. " +
                "'internal(revision=3,4,5)'")
    String fromRepository = "";
    @Option(name = "--in_repository", required = true,
            usage = "Which repository to find equivalences in")
    String inRepository = "";
  }
}
