// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionGraph;
import com.google.devtools.moe.client.repositories.RevisionMatcher;

import java.util.List;
import java.util.Set;

/**
 * {@link RevisionMatcher} that matches on {@link Revision}s for which there is an
 * {@link Equivalence} in the given {@link Db}.
 *
 */
public class EquivalenceMatcher
    implements RevisionMatcher<EquivalenceMatcher.EquivalenceMatchResult> {

  /** The name of the Repository _other_ than that of Revisions checked in matches(). */
  private final String repositoryName;

  private final Db db;

  public EquivalenceMatcher(String repositoryName, Db db) {
    this.repositoryName = repositoryName;
    this.db = db;
  }

  @Override
  public boolean matches(Revision revision) {
    return !db.findEquivalences(revision, repositoryName).isEmpty();
  }

  @Override
  public EquivalenceMatchResult makeResult(RevisionGraph nonMatching, List<Revision> matching) {
    ImmutableList.Builder<Equivalence> equivsBuilder = ImmutableList.builder();
    for (Revision matchRev : matching) {
      Set<Revision> equivRevs = db.findEquivalences(matchRev, repositoryName);
      if (!equivRevs.isEmpty()) {
        equivsBuilder.add(Equivalence.create(matchRev, equivRevs.iterator().next()));
      }
    }
    return new EquivalenceMatchResult(nonMatching, equivsBuilder.build());
  }

  @Override
  public String toString() {
    return "EquivalenceMatcher for repository " + repositoryName;
  }

  /**
   * The result of crawling a revision history with a {@link EquivalenceMatcher}. Stores the
   * revisions found since any equivalence, and the equivalences themselves.
   */
  public static class EquivalenceMatchResult {

    private final RevisionGraph revisionsSinceEquivalence;
    private final List<Equivalence> equivalences;

    EquivalenceMatchResult(
        RevisionGraph revisionsSinceEquivalence, List<Equivalence> equivalences) {
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
    public List<Equivalence> getEquivalences() {
      return equivalences;
    }
  }
}
