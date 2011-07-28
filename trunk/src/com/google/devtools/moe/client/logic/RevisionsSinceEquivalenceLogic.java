// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;

import java.util.Iterator;
import java.util.Set;

/**
 * Performs the logic of the RevisionsSinceEquivalenceDirective.
 *
 */
public class RevisionsSinceEquivalenceLogic {

  /**
   * Prints the revisions since the last equivalence.
   *
   * @param toRepo a String of the name of the repo to check for revisions
   * @param rev the Revision to find the equivalence for
   * @param db the Db to consult for equivalences
   * @param rh the RevisionHistory to consult for revisions
   */
  public static void printRevisionsSinceEquivalence
      (String toRepo, Revision rev, Db db, RevisionHistory rh) {
    RevisionMatcher matcher = new EquivalenceMatcher(toRepo, db);
    Set<Revision> revisions = rh.findRevisions(rev, matcher);
    StringBuilder result = new StringBuilder();
    Iterator<Revision> it = revisions.iterator();
    while (it.hasNext()) {
      result.append(it.next().toString());
      if (it.hasNext()) {
        result.append(", ");
      }
    }
    AppContext.RUN.ui.info("Revisions found: " + result.toString());
  }
}
