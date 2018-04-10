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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.RevisionMetadata.FieldParsingResult;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * A skeletal implementation of {@link RevisionHistory} with common logic.
 */
public abstract class AbstractRevisionHistory implements RevisionHistory {

  private static final int MAX_REVISIONS_TO_SEARCH = 400;

  @Override
  public final RevisionMetadata getMetadata(Revision revision) {
    RevisionMetadata unparsedMetadata = createMetadata(revision);
    if (unparsedMetadata == null) {
      return null;
    }
    FieldParsingResult parseResult = parseFields(unparsedMetadata);
    RevisionMetadata.Builder builder = unparsedMetadata.toBuilder();
    builder.description(parseResult.description());
    builder.fieldsBuilder().putAll(parseResult.fields());
    return builder.build();
  }

  /** The actual creation logic for a {@link RevisionMetadata}, implemented by each repo type */
  protected abstract RevisionMetadata createMetadata(Revision revision);

  /**
   * Field-parsing logic, which extracts fields from the revision metadata description and returns a
   * result containing the stripped description, and a multimap of fields
   */
  protected abstract FieldParsingResult parseFields(RevisionMetadata metadata);

  @Override
  public <T> T findRevisions(Revision revision, RevisionMatcher<T> matcher, SearchType searchType) {

    List<Revision> startingRevisions =
        (revision == null) ? findHeadRevisions() : ImmutableList.of(revision);

    if (startingRevisions.size() > 1 && searchType == SearchType.LINEAR) {
      throw new MoeProblem(
          "MOE found a repository (%s) with multiple heads while trying to search linear history.",
          startingRevisions.get(0).repositoryName());
    }

    RevisionGraph.Builder nonMatchingBuilder = RevisionGraph.builder(startingRevisions);
    ImmutableList.Builder<Revision> matchingBuilder = ImmutableList.builder();

    Deque<Revision> workList = new ArrayDeque<>();
    workList.addAll(startingRevisions);

    // Keep a visited list to make sure we don't visit the same change twice.
    Set<Revision> visited = Sets.newLinkedHashSet();
    visited.addAll(startingRevisions);

    while (!workList.isEmpty()) {
      Revision current = workList.removeFirst();
      if (!matcher.matches(current)) {
        RevisionMetadata metadata = getMetadata(current);
        nonMatchingBuilder.addRevision(current, metadata);

        List<Revision> parentsToSearch = metadata.parents();
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
              "Couldn't find a matching revision for matcher (%s) from %s within %d revisions.",
              matcher,
              (revision == null) ? "head" : revision,
              MAX_REVISIONS_TO_SEARCH);
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
