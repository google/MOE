/*
 * Copyright (c) 2015 Google, Inc.
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

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.writer.WriterCreator;

/**
 * Represents a specific type of repository, including the utilities needed to manipulate it.
 *
 * <p>A {@code RepositoryType} implies a single revision history, and so is specific to a single
 * branch.  Two branches from the same repository will be represented by two distinct
 * {@code RepositoryType} instances.
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


    /**
     * Validates that the supplied
     * {@link com.google.devtools.moe.client.repositories.RepositoryType.Factory} targets the
     * correct repo type, throwing an {@link InvalidProject} exception if it does not.
     */
    default void checkType(RepositoryConfig config) throws InvalidProject {
      if (!this.type().equals(config.getType())) {
        // TODO(cgruber): Make it so this can't happen at runtime, ever, and throw AssertionError.
        throw new InvalidProject(
                "Invalid repository type '%s' for %s",
                config.getType(),
                this.getClass().getSimpleName());
      }
    }

  }
}
