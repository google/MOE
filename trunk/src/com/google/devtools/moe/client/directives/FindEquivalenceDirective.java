// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionEvaluator;
import com.google.devtools.moe.client.repositories.RevisionExpression.RevisionExpressionError;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
    if (options.dbLocation.isEmpty()) {
      AppContext.RUN.ui.error("No --db specified.");
      return 1;
    }
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
    if (options.configFilename.isEmpty()) {
      AppContext.RUN.ui.error("No --config_file specified.");
      return 1;
    }

    ProjectContext context;
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }

    if (options.revision.isEmpty()) {
      AppContext.RUN.ui.error("No --revision specified");
      return 1;
    }

    if (options.inRepository.isEmpty()) {
      AppContext.RUN.ui.error("No --in_repository specified");
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
    for (Revision rev : revs) {
      Set<Revision> equivalences = db.findEquivalences(rev, options.inRepository);
      StringBuilder result = new StringBuilder();
      Iterator<Revision> it = equivalences.iterator();
      while (it.hasNext()) {
        result.append(it.next().revId);
        if (it.hasNext()) {
          result.append(",");
        }
      }
      if (equivalences.isEmpty()) {
        AppContext.RUN.ui.info(
            NoEquivalenceBuilder(rev.repositoryName, rev.revId, options.inRepository));
      } else {
        AppContext.RUN.ui.info(
          EquivalenceBuilder(rev.repositoryName, rev.revId,
              options.inRepository, result.toString()));
      }
    }
    return 0;
  }

  /**
   * Builds a string to be displayed when no equivalences were found.
   * Ex) No equivalences for "googlecode{3}" in repository "internal"
   */
  public static String NoEquivalenceBuilder(String repoName, String revId, String inRepoName) {
    return "No equivalences for \"" + repoName + "{" + revId + "}\"" +
        " in repository \"" + inRepoName + "\"";
  }

  /**
   * Builds a string to display the equivalences.
   * Ex) "internal{35}" == "googlecode{14,15}"
   */
  public static String EquivalenceBuilder(String repoName, String revId,
      String inRepoName, String equivRevIds) {
    return "\"" + repoName + "{" + revId + "}\" == \"" +
        inRepoName + "{" + equivRevIds + "}\"";
  }

  static class FindEquivalenceOptions extends MoeOptions {
    @Option(name = "--config_file",
      usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--db",
            usage = "Location of MOE database")
    String dbLocation = "";
    @Option(name = "--revision",
            usage = "A revision expression for which revision to find equivalences for")
    String revision = "";
    @Option(name = "--in_repository",
            usage = "Which repository to find equivalences in")
    String inRepository = "";
  }
}
