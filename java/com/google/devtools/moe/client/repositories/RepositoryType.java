// Copyright 2015 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.writer.WriterCreator;

/**
 * Represents a specific type of repository, including the utilities needed to manipulate it.
 *
 * <p>A {@code RepositoryType} implies a single revision history, and so is specific to a single
 * branch.  Two branches from the same repository will be represented by two distinct
 * {@code RepositoryType} instances.
 *
 * @author cgruber@google.com (Christian Gruber)
 */
@AutoValue
public abstract class RepositoryType {
  public abstract String name();

  public abstract RevisionHistory revisionHistory();

  public abstract CodebaseCreator codebaseCreator();

  public abstract WriterCreator writerCreator();

  public static RepositoryType create(
      String name,
      RevisionHistory revisionHistory,
      CodebaseCreator codebaseCreator,
      WriterCreator writerCreator) {
    return new AutoValue_RepositoryType(name, revisionHistory, codebaseCreator, writerCreator);
  }

  /**
   * A keyed factory type which knows how to build a {@link RepositoryType} of the type
   * indicated by its {@link Factory#type()} method.
   */
  public interface Factory {
    /** A string representation of this type of repository (e.g. {@code "svn"} or {@code "git"}). */
    String type();

    /**
     * Creates a {@link RepositoryType} of the appropriate type (git, svn, etc.)
     */
    // TODO(cgruber): Consider making this an abstract class with a template method for validation.
    RepositoryType create(String name, RepositoryConfig config) throws InvalidProject;
  }
}
