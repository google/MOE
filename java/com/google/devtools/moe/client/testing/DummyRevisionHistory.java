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

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionGraph;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import org.joda.time.DateTime;

/**
 * A fake implementation of {@link RevisionHistory} for testing.
 *
 * <p>Constructors take an optional number of "canned" commits in order to simulate a real
 * repository.  If there are no commits given, the history will either behave as an empty
 * repository and return nulls for method calls that search for commits or metadata, or (if
 * created in legacy mode) will return some default commit information, to support legacy tests.
 *
 * <p>Example usage (simulating a monotonically increasing repository):<pre> {@code
 *
 *     DummyCommit c1 = DummyCommit.create("1", "foo@email.com", "description 1", aDateTime);
 *     DummyCommit c2 = DummyCommit.create("2", "foo@email.com", "description 2", aDateTime, c1);
 *     DummyCommit c3 = DummyCommit.create("3", "foo@email.com", "description 3", aDateTime, c2);
 *     RevisionHistory history =
 *         DummyRevisionHistory.builder()
 *             .name("myrepo")
 *             .permissive(false) // strict
 *             .add(c1, c2, c3)
 *             .build();
 * }</pre>
 *
 * <p>The above creates a revision history with three commits in series. Commits can be arranged
 * in linear chains or directed graphs representing branch/merge lines. Revision numbers can be
 * any string, but will typically be a numeric string representing monotonically increasing
 * version numbers, or commit hash ids.
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
  public RevisionMetadata getMetadata(Revision revision) throws MoeProblem {
    if (!name().equals(revision.repositoryName())) {
      throw new MoeProblem(
          String.format(
              "Could not get metadata: Revision %s is in repository %s instead of %s",
              revision.revId(),
              revision.repositoryName(),
              name()));
    }
    DummyCommit commit = indexedCommits().get(revision.revId());
    if (commit != null) {
      ImmutableList<Revision> parents =
          FluentIterable.from(commit.parents())
              .transform(
                  new Function<DummyCommit, Revision>() {
                    @Override
                    public Revision apply(DummyCommit input) {
                      return Revision.create(input.id(), name());
                    }
                  })
              .toList();
      return new RevisionMetadata(
          commit.id(), commit.author(), commit.timestamp(), commit.description(), parents);
    }
    // Support legacy tests.
    if (permissive()) {
      return new RevisionMetadata(
          revision.revId(),
          "author",
          new DateTime(1L),
          revision.revId().equals("migrated_to")
              ? "MOE_MIGRATED_REVID=migrated_from"
              : "description",
          ImmutableList.of(Revision.create("parent", name())));
    }
    return null;
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

  public abstract Builder toBuilder();

  /**
   * Builds a {@link DummyRevisionHistory} with a set of canned {@link DummyCommit} instances.
   */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String name);

    public abstract Builder permissive(boolean permissive);

    abstract ImmutableMap.Builder<String, DummyCommit> indexedCommitsBuilder();

    abstract ImmutableList.Builder<DummyCommit> commitsBuilder();

    public Builder add(DummyCommit... commits) {
      addAll(ImmutableList.copyOf(commits));
      return this;
    }

    public Builder addAll(Iterable<DummyCommit> commits) {
      for (DummyCommit commit : commits) {
        commitsBuilder().add(commit);
        indexedCommitsBuilder().put(commit.id(), commit);
      }
      return this;
    }

    public abstract DummyRevisionHistory build();
  }
}
