// Copyright 2012 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.MoeProblem;

import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * A skeletal implementation of {@link RevisionHistory} with common logic.
 *
 */
public abstract class AbstractRevisionHistory implements RevisionHistory {

  private static final int MAX_REVISIONS_TO_SEARCH = 400;

  /**
   * Starting at the given revision, search backwards through the revision history until a matching
   * revision is found. Returns the result of
   * {@link RevisionMatcher#makeResult(RevisionGraph, List)} given the non-matching Revisions as a
   * RevisionGraph, and the matching Revisions in the order encountered.
   *
   * @param revision  the revision to start at. If null, then start at head revision.
   * @param matcher  the {@link RevisionMatcher} to apply
   */
  @Override
  public <T> T findRevisions(Revision revision, RevisionMatcher<T> matcher) {
    List<Revision> startingRevisions =
        (revision == null) ? findHeadRevisions() : ImmutableList.of(revision);
    RevisionGraph.Builder resultBuilder = RevisionGraph.builder(startingRevisions);
    ImmutableList.Builder<Revision> matchingBuilder = ImmutableList.builder();

    Deque<Revision> workList = Lists.newLinkedList();
    workList.addAll(startingRevisions);

    // Keep a visited list to make sure we don't visit the same change twice.
    Set<Revision> visited = Sets.newLinkedHashSet();
    visited.addAll(startingRevisions);

    while (!workList.isEmpty()) {
      Revision current = workList.removeFirst();
      if (!matcher.matches(current)) {
        RevisionMetadata metadata = getMetadata(current);
        resultBuilder.addRevision(current, metadata);

        for (Revision parent : metadata.parents) {
          // Don't add a visited parent to the search queue.
          if (visited.add(parent)) {
            workList.addLast(parent);
          }
        }

        if (visited.size() > MAX_REVISIONS_TO_SEARCH) {
          throw new MoeProblem(String.format(
              "Couldn't find a matching revision for matcher (%s) in repo %s within %d revisions.",
              matcher,
              revision.repositoryName,
              MAX_REVISIONS_TO_SEARCH));
        }
      } else {
        matchingBuilder.add(current);
      }
    }

    return matcher.makeResult(resultBuilder.build(), matchingBuilder.build());
  }

  /**
   * Find all head revisions (e.g. one each for all branches).
   */
  protected abstract List<Revision> findHeadRevisions();
}
