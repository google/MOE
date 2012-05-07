// Copyright 2012 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the Revisions found by crawling a repository history with a {@link RevisionMatcher}, and
 * provides methods to access the result linearly (following only the first parent) or breadth-first
 * from the starting Revision(s).
 *
 */
public class RevisionGraph {

  private final List<Revision> startingRevisions;
  private final Map<Revision, RevisionMetadata> matchingRevsAndMetadata;

  private RevisionGraph(
      List<Revision> startingRevisions, Map<Revision, RevisionMetadata> matchingRevsAndMetadata) {
    this.startingRevisions = startingRevisions;
    this.matchingRevsAndMetadata = matchingRevsAndMetadata;
  }

  /**
   * Returns a linearized revision history result, from the (by assumption, single) starting
   * revision backwards following only the first parent of each revision. In DVCSes, this means that
   * only the first parent of a merge commit -- the commit on the same branch, not the branch merged
   * in -- will be included.
   */
  public List<Revision> getLinearHistory() {
    Preconditions.checkState(
        startingRevisions.size() == 1,
        "Can't get linear history from multiple heads!");

    return getHistory(true);
  }

  /**
   * Returns a breadth-first revision history result, from the starting revisions backwards through
   * all parents not filtered out by the {@code RevisionMatcher}.
   */
  // TODO(user): Switch from List to something else? Iterable?
  public List<Revision> getBreadthFirstHistory() {
    return getHistory(false);
  }

  // TODO(user): When we start to use getBreadthFirstHistory() legitimately, make sure this
  // doesn't visit the same parent N times via its N children (unless it suits our implementation).
  private List<Revision> getHistory(boolean linear) {
    ImmutableList.Builder<Revision> historyBuilder = ImmutableList.builder();
    Deque<Revision> workList = Lists.newLinkedList();
    workList.addAll(startingRevisions);
    while (!workList.isEmpty()) {
      Revision current = workList.removeFirst();
      if (matchingRevsAndMetadata.containsKey(current)) {
        historyBuilder.add(current);
        RevisionMetadata metadata = matchingRevsAndMetadata.get(current);
        if (linear && !metadata.parents.isEmpty()) {
          workList.add(metadata.parents.get(0));
        } else {
          workList.addAll(metadata.parents);
        }
      }
    }
    return historyBuilder.build();
  }

  public static Builder builder(List<Revision> startingRevisions) {
    return new Builder(startingRevisions);
  }

  /**
   * A Builder for building a RevisionGraph. Example:
   *
   * <pre> {@code
   * RevisionGraph.Builder historyBuilder = RevisionGraph.builder(startRev1, startRev2);
   * historyBuilder.addRevision(startRev1, startRev1Metadata);
   * historyBuilder.addRevision(startRev2, startRev2Metadata);
   * historyBuilder.addRevision(startRev1Parent, startRev1ParentMetadata);
   * // And so on...
   * RevisionGraph history = historyBuilder.build();
   * }</pre>
   */
  public static class Builder {

    private final List<Revision> startingRevisions;
    private final HashMap<Revision, RevisionMetadata> matchingRevsAndMetadata = Maps.newHashMap();

    private Builder(List<Revision> startingRevisions) {
      this.startingRevisions = ImmutableList.copyOf(startingRevisions);
    }

    public Builder addRevision(Revision revision, RevisionMetadata metadata) {
      Preconditions.checkState(!matchingRevsAndMetadata.containsKey(revision));
      matchingRevsAndMetadata.put(revision, metadata);
      return this;
    }

    public RevisionGraph build() {
      return new RevisionGraph(startingRevisions, ImmutableMap.copyOf(matchingRevsAndMetadata));
    }
  }
}
