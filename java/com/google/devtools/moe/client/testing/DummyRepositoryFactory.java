/*
 * Copyright (c) 2011 Google, Inc.
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
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionGraph;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.WriterCreator;

import org.joda.time.DateTime;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Creates a simple {@link RepositoryType} for testing.
 */
public class DummyRepositoryFactory implements RepositoryType.Factory {
  private final FileSystem filesystem;

  @Inject
  public DummyRepositoryFactory(@Nullable FileSystem filesystem) {
    this.filesystem = filesystem;
  }

  @Override
  public String type() {
    return "dummy";
  }

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
   *     RevisionHistory history = new DummyRevisionhistory("myrepo", false, c1, c2, c3); // strict
   *
   * }</pre>
   *
   * <p>The above creates a revision history with three commits in series. Commits can be arranged
   * in linear chains or directed graphs representing branch/merge lines. Revision numbers can be
   * any string, but will typically be a numeric string representing monotonically increasing
   * version numbers, or commit hash ids.
   */
  public static class DummyRevisionHistory implements RevisionHistory {
    private final String name;
    private final ImmutableList<DummyCommit> commits;
    private final ImmutableMap<String, DummyCommit> indexedCommits;
    private final boolean permissive; // whether this history returns unknown revisions when asked.

    public DummyRevisionHistory(String name, DummyCommit... commits) {
      this(name, ImmutableList.copyOf(commits));
    }

    public DummyRevisionHistory(String name, boolean permissive, DummyCommit... commits) {
      this(name, permissive, ImmutableList.copyOf(commits));
    }

    public DummyRevisionHistory(String name, List<DummyCommit> commits) {
      this(name, true, commits);
    }

    public DummyRevisionHistory(String name, boolean permissive, List<DummyCommit> commits) {
      this.permissive = permissive;
      this.name = name;
      this.commits = ImmutableList.copyOf(commits);
      this.indexedCommits =
          FluentIterable.from(commits)
              .uniqueIndex(
                  new Function<DummyCommit, String>() {
                    @Override
                    public String apply(DummyCommit input) {
                      return input.id();
                    }
                  });
    }

    @Override
    public Revision findHighestRevision(String revId) {
      if (Strings.isNullOrEmpty(revId)) {
        // Search for head revision
        if (commits.isEmpty()) {
          // if permissive, support legacy tests with an assumed revision.
          if (permissive) {
            return Revision.create("1", name);
          } else {
            return null;
          }
        } else {
          // Construct head revision from head commit.
          return Revision.create(Iterables.getLast(commits).id(), name);
        }
      }

      if (indexedCommits.containsKey(revId)) {
        return Revision.create(revId, name);
      }

      // if permissive, support legacy tests with an assumed revision.
      return (permissive) ? Revision.create(revId, name) : null;
    }

    @Override
    public RevisionMetadata getMetadata(Revision revision) throws MoeProblem {
      if (!name.equals(revision.repositoryName())) {
        throw new MoeProblem(
            String.format(
                "Could not get metadata: Revision %s is in repository %s instead of %s",
                revision.revId(),
                revision.repositoryName(),
                name));
      }
      DummyCommit commit = indexedCommits.get(revision.revId());
      if (commit != null) {
        ImmutableList<Revision> parents =
            FluentIterable.from(commit.parents())
                .transform(
                    new Function<DummyCommit, Revision>() {
                      @Override
                      public Revision apply(DummyCommit input) {
                        return Revision.create(input.id(), name);
                      }
                    })
                .toList();
        return new RevisionMetadata(
            commit.id(), commit.author(), commit.timestamp(), commit.description(), parents);
      }
      // Support legacy tests.
      if (permissive) {
        return new RevisionMetadata(
            revision.revId(),
            "author",
            new DateTime(1L),
            revision.revId().equals("migrated_to")
                ? "MOE_MIGRATED_REVID=migrated_from"
                : "description",
            ImmutableList.of(Revision.create("parent", name)));
      }
      return null;
    }

    @Override
    public <T> T findRevisions(
        Revision revision, RevisionMatcher<T> matcher, SearchType searchType) {
      if (revision == null) {
        revision = Revision.create("migrated_to", name);
      }
      RevisionGraph revTree =
          RevisionGraph.builder(ImmutableList.of(revision))
              .addRevision(revision, getMetadata(revision))
              .build();
      return matcher.makeResult(revTree, ImmutableList.of(Revision.create(1, name)));
    }
  }

  @Override
  public RepositoryType create(String repositoryName, RepositoryConfig config) {
    return create(repositoryName, config, null);
  }

  public RepositoryType create(
      String repositoryName, RepositoryConfig config, ImmutableList<DummyCommit> commits) {
    String projectSpace = null;
    if (config != null) {
      projectSpace = config.getProjectSpace();
    }
    if (projectSpace == null) {
      projectSpace = "public";
    }
    RevisionHistory revisionHistory =
        commits == null
            ? new DummyRevisionHistory(repositoryName)
            : new DummyRevisionHistory(repositoryName, commits);
    CodebaseCreator codebaseCreator =
        new DummyCodebaseCreator(filesystem, repositoryName, projectSpace);
    WriterCreator writerCreator = new DummyWriterCreator(repositoryName);
    return RepositoryType.create(repositoryName, revisionHistory, codebaseCreator, writerCreator);
  }

  /**
   * Internal representation of a repository commit used within a {@link DummyRevisionHistory}.
   */
  @AutoValue
  public abstract static class DummyCommit {
    public abstract String id();

    public abstract String author();

    public abstract String description();

    public abstract DateTime timestamp();

    public abstract ImmutableList<DummyCommit> parents();

    /** returns a commit containing the usual metadata, but with no ancestor(s) */
    public static DummyCommit create(
        String id, String author, String description, DateTime timestamp) {
      return create(id, author, description, timestamp, ImmutableList.<DummyCommit>of());
    }

    /** returns a commit containing the usual metadata and ancestor commit(s) */
    public static DummyCommit create(
        String id, String author, String description, DateTime timestamp, DummyCommit... parents) {
      return new AutoValue_DummyRepositoryFactory_DummyCommit(
          id, author, description, timestamp, ImmutableList.copyOf(parents));
    }

    /** returns a commit containing the usual metadata and a list of ancestor commit(s) */
    public static DummyCommit create(
        String id,
        String author,
        String description,
        DateTime timestamp,
        ImmutableList<DummyCommit> parents) {
      return new AutoValue_DummyRepositoryFactory_DummyCommit(
          id, author, description, timestamp, parents);
    }
  }
}
