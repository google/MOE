// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;

import java.util.List;

/**
 * Performs the logic of the RevisionsSinceEquivalenceDirective.
 *
 */
public class RevisionsSinceEquivalenceLogic {

  /**
   * Finds the revisions in a from-repository since the last equivalence with a to-repository (e.g.
   * for migration).
   *
   * @param fromRepoName  the name of the from-repository
   * @param toRepoName  the name of the to-repository
   * @param db  the Db to consult for equivalences
   * @param context  the ProjectContext
   */
  public static List<Revision> getRevisionsSinceEquivalence(
      String fromRepoName, String toRepoName, Db db, ProjectContext context) {
    Repository fromRepo = context.repositories.get(fromRepoName);
    if (fromRepo == null) {
      throw new MoeProblem("Unknown repository: " + fromRepoName);
    }
    RevisionHistory fromRevisionHistory = fromRepo.revisionHistory;
    RevisionMatcher matcher = new EquivalenceMatcher(toRepoName, db);
    Revision startingFromRevision = null;  // Start search at from-repository head.
    return ImmutableList.copyOf(fromRevisionHistory.findRevisions(startingFromRevision, matcher));
  }
}
