// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * RevisionHistory for DVCS, for example the BFS logic of searching the revision history tree from
 * child to parents.
 *
 */
public abstract class AbstractDvcsRevisionHistory implements RevisionHistory {

  /**
   * Starting at specified revision, recur until a matching revision is found
   *
   * @param revision  the revision to start at.  If null, then start at head revision
   * @param matcher  the matcher to apply
   * @return all non-matching revisions.
   */
  @Override
  public Set<Revision> findRevisions(Revision revision, RevisionMatcher matcher) {
    ImmutableSet.Builder<Revision> resultBuilder = ImmutableSet.builder();

    // Keep a visited list to make sure we don't visit the same change twice.
    Set<Revision> visited = Sets.newLinkedHashSet();
    if (revision == null) {
      visited.addAll(findHeadRevisions());
    } else {
      visited.add(revision);
    }

    Deque<Revision> workList = Lists.newLinkedList();
    workList.addAll(visited);

    while (!workList.isEmpty()) {
      Revision current = workList.removeFirst();
      if (!matcher.matches(current)) {
        resultBuilder.add(current);

        RevisionMetadata metadata = getMetadata(current);
        for (Revision parent : metadata.parents) {
          // Don't add a visited parent to the search queue.
          if (visited.add(parent)) {
            workList.addLast(parent);
          }
        }
      }
    }

    return resultBuilder.build();
  }

  /**
   * Find all head revisions (e.g. one each for all branches).
   */
  protected abstract List<Revision> findHeadRevisions();

  /**
   * Starting at specified revision, do a breadth-first search back through the revision history
   * until an equivalence is found, or there are no more parents to be examined, or if the number of
   * parents that have been examined exceeds RevisionHistory.MAX_PARENTS_TO_EXAMINE.
   *
   * @param revision  the Revision to start at
   * @param matcher  the EquivalenceMatcher to apply
   *
   * @return the most recent Equivalence or null if none was found
   */
  @Override
  public Equivalence findLastEquivalence(Revision revision, EquivalenceMatcher matcher) {
    // Perform a BFS on the revisions and return the first equivalence.
    Deque<Revision> queue = new LinkedList<Revision>();
    queue.add(revision);

    AppContext.RUN.ui.info(String.format("Looking for an equivalence with repository %s starting "
        + "from revision %s...", matcher.repositoryName, revision.toString()));

    int parentsExamined = 0;
    while (!queue.isEmpty()) {
      Revision currRevision = queue.remove();
      // Null is returned if no Equivalence exists for this Revision.
      Equivalence result = matcher.getEquivalence(currRevision);
      if (result != null) {
        return result;
      }
      queue.addAll(getMetadata(currRevision).parents);
      parentsExamined++;
    }
    // The maximum number of parents where examined and an equivalence was not found.
    if (parentsExamined >= MAX_PARENTS_TO_EXAMINE) {
      AppContext.RUN.ui.info(String.format("No equivalence with repository %s starting from "
          + "revision %s was found after examining %d parent revisions. Null was returned.",
          matcher.repositoryName, revision.toString(), MAX_PARENTS_TO_EXAMINE));
      return null;
    }
    // No equivalence was found.
    return null;
  }
}
