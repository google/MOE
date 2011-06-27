// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.DummyDb;

import org.kohsuke.args4j.Option;

import java.util.Iterator;
import java.util.Set;

/**
 * Finds revisions in a respository that are equivalent to a given revision
 *
 */
public class FindEquivalenceDirective implements Directive {

  private final FindEquivalenceOptions options = new FindEquivalenceOptions();

  public FindEquivalenceDirective() {}

  public FindEquivalenceOptions getFlags() {
    return options;
  }

  public int perform() {
    Db db;
    if (options.dbLocation.isEmpty()) {
      System.err.println("No --db specified.");
      return 1;
    }
    if (options.dbLocation.equals("dummy")) {
      db = new DummyDb();
    } else {
      // TODO(user): also allow for url dbLocation types
      try {
        db = new FileDb(FileDb.makeDbFromFile(options.dbLocation));
      } catch (MoeProblem e) {
        System.err.println(e.explanation);
        return 1;
      }
    }

    if (options.revision.isEmpty()) {
      AppContext.RUN.ui.error("No --revision specified");
      return 1;
    }

    if (options.repository.isEmpty()) {
      AppContext.RUN.ui.error("No --repository specified");
      return 1;
    }

    if (options.inRepository.isEmpty()) {
      AppContext.RUN.ui.error("No --in_repository specified");
      return 1;
    }

    Revision rev = new Revision(options.revision, options.repository);
    Set<Revision> equivalences = db.findEquivalences(rev, options.inRepository);
    StringBuilder result = new StringBuilder();
    Iterator<Revision> it = equivalences.iterator();
    while (it.hasNext()) {
      result.append(it.next().revId);
      if (it.hasNext()) {
        result.append(", ");
      }
    }
    //TODO(user): better print format
    AppContext.RUN.ui.info("Revisions in repository \"" + options.inRepository +
                            "\" equivalent to revision \"" + options.revision +
                            "\" in repository \"" + options.repository +
                            "\": " + result.toString());
    return 0;
  }

  static class FindEquivalenceOptions extends MoeOptions {
    @Option(name = "--db",
            usage = "Location of MOE database")
    String dbLocation = "";
    @Option(name = "--revision",
            usage = "Which revision to find equivalences for")
    String revision = "";
    @Option(name = "--repository",
            usage = "Which repository given revision is in")
    String repository = "";
    @Option(name = "--in_repository",
            usage = "Which repository to find equivalences in")
    String inRepository = "";
  }
}
