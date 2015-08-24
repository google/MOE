// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories.noop;

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;
import com.google.devtools.moe.client.writer.WritingError;

import java.util.Map;

import javax.inject.Inject;

/**
 * Creates a simple {@link RepositoryType} for testing.
 */
public class NoopRepositoryFactory implements RepositoryType.Factory {
  private static final String NOOP_NOT_VALID =
      "No-op repository \"none\" invalid for directives "
          + "that expect to interact with a real repository";

  @Inject
  public NoopRepositoryFactory() {}

  @Override
  public String type() {
    return "none";
  }

  /** A fake implementation of {@link RevisionHistory} for testing. */
  static class NoOpRevisionHistory implements RevisionHistory {
    @Override
    public Revision findHighestRevision(String revId) {
      throw new MoeProblem(NOOP_NOT_VALID);
    }

    @Override
    public RevisionMetadata getMetadata(Revision revision) {
      throw new MoeProblem(NOOP_NOT_VALID);
    }

    @Override
    public <T> T findRevisions(
        Revision revision, RevisionMatcher<T> matcher, SearchType searchType) {
      throw new MoeProblem(NOOP_NOT_VALID);
    }
  }

  @Override
  public RepositoryType create(String name, RepositoryConfig config) throws InvalidProject {
    return RepositoryType.create(
        name,
        new NoOpRevisionHistory(),
        new CodebaseCreator() {
          @Override
          public Codebase create(Map<String, String> options) throws CodebaseCreationError {
            throw new MoeProblem(NOOP_NOT_VALID);
          }
        },
        new WriterCreator() {
          @Override
          public Writer create(Map<String, String> options) throws WritingError {
            throw new MoeProblem(NOOP_NOT_VALID);
          }
        });
  }
}
