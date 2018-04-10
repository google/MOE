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

package com.google.devtools.moe.client.migrations;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.translation.pipeline.TranslationPipeline;
import java.util.List;

/**
 * A Migration represents a change to be ported from one {@link RepositoryType} to another
 * via a {@link TranslationPipeline}.
 */
@AutoValue
public abstract class Migration {

  /** Returns the symbolic name of the migration (For user output). */
  protected abstract String name();

  /** Returns the name of the repository from which the revision is migrated. */
  public abstract String fromRepository();

  /** Returns the name of the repository to which the revision is migrated. */
  public abstract String toRepository();

  /** Returns the most recent Equivalence b/w the from and to repos of this Migration */
  public abstract RepositoryEquivalence sinceEquivalence();

  /** Returns the changes in fromRepository encapsulated by this Migration */
  public abstract List<Revision> fromRevisions();

  /**
   * Creates an object that represents a specific migration operation.
   *
   * @param name symbolic name of the migration (For user output)
   * @param fromRepository the name of the repository from which the revision is migrated.
   * @param toRepository the name of the repository to which the revision is migrated.
   * @param fromRevisions the most recent Equivalence b/w the from and to repos of this Migration
   * @param sinceEquivalence the changes in fromRepository encapsulated by this Migration
   */
  public static Migration create(
      String name,
      String fromRepository,
      String toRepository,
      List<Revision> fromRevisions,
      RepositoryEquivalence sinceEquivalence) {
    return new AutoValue_Migration(
        name, fromRepository, toRepository, sinceEquivalence, fromRevisions);
  }

  @Override
  public String toString() {
    return String.format(
        "Migration %s since equivalence (%s) of revisions {%s}",
        name(),
        sinceEquivalence(),
        Joiner.on(", ").join(fromRevisions()));
  }
}
