/*
 * Copyright (c) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores the Revisions found by crawling a repository history with a {@link RevisionMatcher}.
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
   * Returns a breadth-first revision history result, from the starting revisions backwards through
   * all parents not filtered out by the {@code RevisionMatcher}.
   */
  public List<Revision> getBreadthFirstHistory() {
    ImmutableList.Builder<Revision> historyBuilder = ImmutableList.builder();
    Set<Revision> seen = new HashSet<Revision>();
    Deque<Revision> workList = new ArrayDeque<>();
    workList.addAll(startingRevisions);
    while (!workList.isEmpty()) {
      Revision current = workList.removeFirst();
      if (seen.add(current)) {
        if (matchingRevsAndMetadata.containsKey(current)) {
          historyBuilder.add(current);
          RevisionMetadata metadata = matchingRevsAndMetadata.get(current);
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
