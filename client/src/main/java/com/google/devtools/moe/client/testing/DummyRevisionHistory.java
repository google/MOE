/*
 * Copyright (c) 2016 Google, Inc.
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
package com.google.devtools.moe.client.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionGraph;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.repositories.RevisionMetadata.FieldParsingResult;
import java.util.HashMap;
import org.joda.time.DateTime;

/**
 * A fake implementation of {@link RevisionHistory} for testing.
 *
 * <p>Constructors take an optional number of "canned" commits in order to simulate a real
 * repository. If there are no commits given, the history will either behave as an empty repository
 * and return nulls for method calls that search for commits or metadata, or (if created in legacy
 * mode) will return some default commit information, to support legacy tests.
 *
 * <p>Example usage (simulating a monotonically increasing repository):
 *
 * <pre>{@code
 * DummyCommit c1 = DummyCommit.create("1", "foo@email.com", "description 1", aDateTime);
 * DummyCommit c2 = DummyCommit.create("2", "foo@email.com", "description 2", aDateTime, c1);
 * DummyCommit c3 = DummyCommit.create("3", "foo@email.com", "description 3", aDateTime, c2);
 * RevisionHistory history =
 *     DummyRevisionHistory.builder()
 *         .name("myrepo")
 *         .permissive(false) // strict
 *         .add(c1, c2, c3)
 *         .build();
 * }</pre>
 *
 * <p>The above creates a revision history with three commits in series. Commits can be arranged in
 * linear chains or directed graphs representing branch/merge lines. Revision numbers can be any
 * string, but will typically be a numeric string representing monotonically increasing version
 * numbers, or commit hash ids.
 */
@AutoValue
public abstract class DummyRevisionHistory implements RevisionHistory {
  public abstract String name();

  public abstract ImmutableList<DummyCommit> commits();

  public abstract ImmutableMap<String, DummyCommit> indexedCommits();

  public abstract boolean permissive(); // does this history returns unknown revisions if asked?

  @Override
  public Revision findHighestRevision(String revId) {
    if (Strings.isNullOrEmpty(revId)) {
      // Search for head revision
      if (commits().isEmpty()) {
        // if permissive, support legacy tests with an assumed revision.
        if (permissive()) {
          return Revision.create("1", name());
        } else {
          return null;
        }
      } else {
        // Construct head revision from head commit.
        return Revision.create(Iterables.getLast(commits()).id(), name());
      }
    }

    if (indexedCommits().containsKey(revId)) {
      return Revision.create(revId, name());
    }

    // if permissive, support legacy tests with an assumed revision.
    return (permissive()) ? Revision.create(revId, name()) : null;
  }

  @Override
  public final RevisionMetadata getMetadata(Revision revision) {
    RevisionMetadata unparsedMetadata = createMetadata(revision);
    return parseLegacyFields(unparsedMetadata);
  }

  RevisionMetadata createMetadata(Revision revision) {
    if (!name().equals(revision.repositoryName())) {
      throw new MoeProblem(
              "Could not get metadata: Revision %s is in repository %s instead of %s",
              revision.revId(),
              revision.repositoryName(),
              name());
    }
    DummyCommit commit = indexedCommits().get(revision.revId());
    if (commit != null) {
      ImmutableList<Revision> parents =
          commit
              .parents()
              .stream()
              .map(input -> Revision.create(input.id(), name()))
              .collect(toImmutableList());
      return RevisionMetadata.builder()
          .id(commit.id())
          .author(commit.author())
          .date(commit.timestamp())
          .description(commit.description())
          .withParents(parents)
          .build();
    }
    // Support legacy tests.
    if (permissive()) {
      return RevisionMetadata.builder()
          .id(revision.revId())
          .author("author")
          .date(new DateTime(1L))
          .description(
              revision.revId().equals("migrated_to")
                  ? "MOE_MIGRATED_REVID=migrated_from"
                  : "description")
          .withParents(Revision.create("parent", name()))
          .build();
    }
    return null;
  }

  /**
   * Lets tests conveniently parse fields as the legacy behavior indicates, without having to create
   * metadata through this {@link RevisionHistory} subtype.
   */
  public static RevisionMetadata parseLegacyFields(RevisionMetadata unparsedMetadata) {
    if (unparsedMetadata == null) {
      return null;
    }
    FieldParsingResult parseResult =
        RevisionMetadata.legacyFieldParser(unparsedMetadata.description());
    RevisionMetadata.Builder builder = unparsedMetadata.toBuilder();
    builder.description(parseResult.description());
    builder.fieldsBuilder().putAll(parseResult.fields());
    return builder.build();
  }

  @Override
  public <T> T findRevisions(Revision revision, RevisionMatcher<T> matcher, SearchType searchType) {
    if (revision == null) {
      revision = Revision.create("migrated_to", name());
    }
    RevisionGraph revTree =
        RevisionGraph.builder(ImmutableList.of(revision))
            .addRevision(revision, getMetadata(revision))
            .build();
    return matcher.makeResult(revTree, ImmutableList.of(Revision.create(1, name())));
  }

  public static Builder builder() {
    return new AutoValue_DummyRevisionHistory.Builder().permissive(true);
  }

  abstract Builder toBuilder();

  public Builder extend() {
    return toBuilder().syncRunningIndex();
  }

  /**
   * Builds a {@link DummyRevisionHistory} with a set of canned {@link DummyCommit} instances.
   */
  @AutoValue.Builder
  public abstract static class Builder {
    private final HashMap<String, DummyCommit> runningIndex = new HashMap<>();

    private Builder syncRunningIndex() {
      // For the copy-builder - set the index up from existing commits so it's ready
      // for follow-on commits.
      runningIndex.clear();
      runningIndex.putAll(indexedCommitsBuilder().build());
      return this;
    }

    public abstract Builder name(String name);

    public abstract Builder permissive(boolean permissive);

    abstract ImmutableMap.Builder<String, DummyCommit> indexedCommitsBuilder();

    abstract Builder indexedCommits(ImmutableMap<String, DummyCommit> index);

    abstract ImmutableList.Builder<DummyCommit> commitsBuilder();

    /**
     * Adds a commit with the supplied details, but looks up the parents by Id from the already
     * registered parents.
     */
    public final Builder add(
        String id, String author, String description, DateTime timestamp, String... parentIds) {
      DummyCommit[] parents = new DummyCommit[parentIds.length];
      for (int i = 0; i < parents.length; i++) {
        DummyCommit parent = runningIndex.get(parentIds[i]);
        if (parent == null) {
          throw new IllegalArgumentException(
              "Attempted to set a commit whose parent hasn't been registered");
        }
        parents[i] = parent;
      }
      add(DummyCommit.create(id, author, description, timestamp, parents));
      return this;
    }

    public final Builder add(DummyCommit... commits) {
      addAll(ImmutableList.copyOf(commits));
      return this;
    }

    public final Builder addAll(Iterable<DummyCommit> commits) {
      for (DummyCommit commit : commits) {
        commitsBuilder().add(commit);
        if (runningIndex.put(commit.id(), commit) != null) {
          throw new IllegalArgumentException(
              "A commit with id '"
                  + commit.id()
                  + "' has already been registered in this history.");
        }
      }
      return this;
    }

    abstract DummyRevisionHistory internalBuild();

    public DummyRevisionHistory build() {
      HashMap<String, DummyCommit> index = new HashMap<>(runningIndex);
      for (String key : indexedCommitsBuilder().build().keySet()) {
        index.remove(key);
      }
      indexedCommitsBuilder().putAll(index);
      return internalBuild();
    }
  }
}
