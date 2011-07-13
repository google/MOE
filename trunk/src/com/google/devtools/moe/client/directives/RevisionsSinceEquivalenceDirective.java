// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionExpression;
import com.google.devtools.moe.client.repositories.RevisionExpression.RevisionExpressionError;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.Iterator;
import java.util.Set;

/**
 * Get all Revisions since last Equivalence
 *
 */
public class RevisionsSinceEquivalenceDirective implements Directive {

  private final RevisionsSinceEquivalenceOptions options = new RevisionsSinceEquivalenceOptions();

  public RevisionsSinceEquivalenceOptions getFlags() {
    return options;
  }
  public RevisionsSinceEquivalenceDirective() {}

  public int perform() {
    ProjectContext context;
    if (options.configFilename.isEmpty()) {
      AppContext.RUN.ui.error("No --config_file specified.");
      return 1;
    }
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e.explanation);
      return 1;
    }

    Db db;
    if (options.dbLocation.isEmpty()) {
      System.err.println("No --db specified.");
      return 1;
    }
    if (options.dbLocation.equals("dummy")) {
      db = new DummyDb(false);
    } else {
      // TODO(user): also allow for url dbLocation types
      try {
        db = new FileDb(FileDb.makeDbFromFile(options.dbLocation));
      } catch (MoeProblem e) {
        System.err.println(e.explanation);
        return 1;
      }
    }

    RevisionExpression re;
    if (options.fromRepository.isEmpty()) {
      System.err.println("No --from_repository specified.");
      return 1;
    }
    try {
      re = RevisionExpression.parse(options.fromRepository);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }
    Repository r = context.repositories.get(re.repoId);
    if (r == null) {
      AppContext.RUN.ui.error("No repository" + re.repoId + ".");
      return 1;
    }
    RevisionHistory rh = r.revisionHistory;
    if (rh == null) {
      AppContext.RUN.ui.error("Repository " + r.name + " does not support revision history.");
      return 1;
    }
    Revision rev;
    if (re.revIds.isEmpty()) {
      rev = rh.findHighestRevision("");
    } else if (re.revIds.size() == 1) {
      rev = rh.findHighestRevision(re.revIds.get(0));
    } else {
      AppContext.RUN.ui.error("Only one revision can be specified for this directive.");
      return 1;
    }

    if (options.toRepository.isEmpty()) {
      AppContext.RUN.ui.error("No --to_repository specified.");
      return 1;
    }
    RevisionMatcher matcher = new EquivalenceMatcher(options.toRepository, db);

    Set<Revision> revisions = rh.findRevisions(rev, matcher);
    StringBuilder result = new StringBuilder();
    Iterator<Revision> it = revisions.iterator();
    while (it.hasNext()) {
      result.append(it.next().toString());
      if (it.hasNext()) {
        result.append(", ");
      }
    }
    AppContext.RUN.ui.info("Revisions found: " +
                            result.toString());
    return 0;
  }

  static class RevisionsSinceEquivalenceOptions extends MoeOptions {
    @Option(name = "--config_file",
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--db",
            usage = "Location of MOE database")
    String dbLocation = "";
    @Option(name = "--from_repository",
            usage = "Revision expression describing repository to find Revisions in")
    String fromRepository = "";
    @Option(name = "--to_repository",
            usage = "Name of Repository to check for Equivalences in")
    String toRepository = "";
  }
}
