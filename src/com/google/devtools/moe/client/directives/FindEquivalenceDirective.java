// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.FindEquivalenceLogic;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionEvaluator;
import com.google.devtools.moe.client.repositories.RevisionExpression.RevisionExpressionError;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.List;

/**
 * Finds revisions in a respository that are equivalent to a given revision
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
        db = new FileDb(FileDb.makeDbFromFile(options.dbLocation));
      } catch (MoeProblem e) {
        AppContext.RUN.ui.error(e.explanation);
        return 1;
      }
    }

    ProjectContext context;
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }

    List<Revision> revs;
    try {
      revs = RevisionEvaluator.parseAndEvaluate(options.revision, context);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }
    if (revs.isEmpty()) {
      AppContext.RUN.ui.error("No revision ids specified in Revision Expression: "
          + options.revision);
      return 1;
    }

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
    @Option(name = "--revision", required = true,
            usage = "A revision expression for which revision to find equivalences for")
    String revision = "";
    @Option(name = "--in_repository", required = true,
            usage = "Which repository to find equivalences in")
    String inRepository = "";
  }
}
