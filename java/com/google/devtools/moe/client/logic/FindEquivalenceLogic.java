// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.repositories.Revision;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Performs the logic of the FindEquivalenceDirective.
 *
 */
public class FindEquivalenceLogic {

  /**
   * Prints the revisions in inRepo that the revisions in revs are equivalent to.
   *
   * @param revs a list of Revisions to find equivalences for
   * @param inRepo the String of the name of the repository to look for equivalences in
   * @param db the database to consult for equivalences
   */
  public static void printEquivalences(List<Revision> revs, String inRepo, Db db) {
    for (Revision rev : revs) {
      Set<Revision> equivalences = db.findEquivalences(rev, inRepo);
      StringBuilder result = new StringBuilder();
      Iterator<Revision> it = equivalences.iterator();
      while (it.hasNext()) {
        result.append(it.next().revId);
        if (it.hasNext()) {
          result.append(",");
        }
      }
      if (equivalences.isEmpty()) {
        Injector.INSTANCE.ui().info(noEquivalenceBuilder(rev.repositoryName, rev.revId, inRepo));
      } else {
        Injector.INSTANCE
            .ui()
            .info(equivalenceBuilder(rev.repositoryName, rev.revId, inRepo, result.toString()));
      }
    }
  }

  /**
   * Builds a string to be displayed when no equivalences were found.
   * Ex) No equivalences for "googlecode{3}" in repository "internal"
   */
  public static String noEquivalenceBuilder(String repoName, String revId, String inRepoName) {
    return "No equivalences for \""
        + repoName
        + "{"
        + revId
        + "}\""
        + " in repository \""
        + inRepoName
        + "\"";
  }

  /**
   * Builds a string to display the equivalences.
   * Ex) "internal{35}" == "googlecode{14,15}"
   */
  public static String equivalenceBuilder(
      String repoName, String revId, String inRepoName, String equivRevIds) {
    return "\"" + repoName + "{" + revId + "}\" == \"" + inRepoName + "{" + equivRevIds + "}\"";
  }
}
