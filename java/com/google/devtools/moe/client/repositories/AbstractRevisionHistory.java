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

  @Override
  public <T> T findRevisions(Revision revision, RevisionMatcher<T> matcher, SearchType searchType) {

    List<Revision> startingRevisions =
        (revision == null) ? findHeadRevisions() : ImmutableList.of(revision);

    if (startingRevisions.size() > 1 && searchType == SearchType.LINEAR) {
      throw new MoeProblem(
          "MOE found a repository (%s) with multiple heads while trying to search linear history.",
          startingRevisions.get(0).repositoryName);
    }

    RevisionGraph.Builder nonMatchingBuilder = RevisionGraph.builder(startingRevisions);
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
        nonMatchingBuilder.addRevision(current, metadata);

        List<Revision> parentsToSearch = metadata.parents;
        if (parentsToSearch.size() > 0 && searchType == SearchType.LINEAR) {
          parentsToSearch = parentsToSearch.subList(0, 1);
        }
        for (Revision parent : parentsToSearch) {
          // Don't add a visited parent to the search queue.
          if (visited.add(parent)) {
            workList.addLast(parent);
          }
        }

        if (visited.size() > MAX_REVISIONS_TO_SEARCH) {
          throw new MoeProblem(
              String.format(
                  "Couldn't find a matching revision for matcher (%s) from %s within %d revisions.",
                  matcher,
                  (revision == null) ? "head" : revision,
                  MAX_REVISIONS_TO_SEARCH));
        }
      } else {
        // Don't search past matching revisions.
        matchingBuilder.add(current);
      }
    }

    return matcher.makeResult(nonMatchingBuilder.build(), matchingBuilder.build());
  }

  /**
   * Find all head revisions (e.g. one each for all branches).
   */
  protected abstract List<Revision> findHeadRevisions();
}
