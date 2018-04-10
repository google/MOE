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

package com.google.devtools.moe.client.database;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionGraph;
import com.google.devtools.moe.client.repositories.RevisionMatcher;

import java.util.List;
import java.util.Set;

/**
 * {@link RevisionMatcher} that matches on {@link Revision}s for which there is an
 * {@link RepositoryEquivalence} in the given {@link Db}.
 */
public class RepositoryEquivalenceMatcher
    implements RevisionMatcher<RepositoryEquivalenceMatcher.Result> {

  /** The name of the Repository _other_ than that of Revisions checked in matches(). */
  private final String repositoryName;

  private final Db db;

  public RepositoryEquivalenceMatcher(String repositoryName, Db db) {
    this.repositoryName = repositoryName;
    this.db = db;
  }

  @Override
  public boolean matches(Revision revision) {
    return !db.findEquivalences(revision, repositoryName).isEmpty();
  }

  @Override
  public RepositoryEquivalenceMatcher.Result makeResult(
      RevisionGraph nonMatching, List<Revision> matching) {
    ImmutableList.Builder<RepositoryEquivalence> equivsBuilder = ImmutableList.builder();
    for (Revision matchRev : matching) {
      Set<Revision> equivRevs = db.findEquivalences(matchRev, repositoryName);
      if (!equivRevs.isEmpty()) {
        equivsBuilder.add(RepositoryEquivalence.create(matchRev, equivRevs.iterator().next()));
      }
    }
    return new RepositoryEquivalenceMatcher.Result(nonMatching, equivsBuilder.build());
  }

  @Override
  public String toString() {
    return "EquivalenceMatcher for repository " + repositoryName;
  }

  /**
   * The result of crawling a revision history with a {@link RepositoryEquivalenceMatcher}.
   * Stores the revisions found since any equivalence, and the equivalences themselves.
   */
  public static class Result {

    private final RevisionGraph revisionsSinceEquivalence;
    private final List<RepositoryEquivalence> equivalences;

    Result(RevisionGraph revisionsSinceEquivalence, List<RepositoryEquivalence> equivalences) {
      this.revisionsSinceEquivalence = revisionsSinceEquivalence;
      this.equivalences = ImmutableList.copyOf(equivalences);
    }

    /**
     * Returns a {@link RevisionGraph} of Revisions since equivalence, or since the start of repo
     * history if no equivalence was found.
     */
    public RevisionGraph getRevisionsSinceEquivalence() {
      return revisionsSinceEquivalence;
    }

    /**
     * Returns the Equivalences found.
     */
    public List<RepositoryEquivalence> getEquivalences() {
      return equivalences;
    }
  }
}
