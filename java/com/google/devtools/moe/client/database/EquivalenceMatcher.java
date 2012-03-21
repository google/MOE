// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMatcher;

/**
 * {@link RevisionMatcher} that matches on {@link Revision}s for which there is an
 * {@link Equivalence} in the given {@link Db}.
 *
 */
public class EquivalenceMatcher implements RevisionMatcher {

  /** The name of the Repository _other_ than that of Revisions checked in matches(). */
  public final String repositoryName;
  public final Db db;

  public EquivalenceMatcher(String repositoryName, Db db) {
    this.repositoryName = repositoryName;
    this.db = db;
  }

  @Override
  public boolean matches(Revision revision) {
    return !db.findEquivalences(revision, repositoryName).isEmpty();
  }

  /**
   * Returns an Equivalence containing the given Revision and its equivalent Revision in the other
   * repository if it exists, otherwise null is returned.
   */
  public Equivalence getEquivalence(Revision revision) {
    if (matches(revision)) {
      return new Equivalence(revision,
          db.findEquivalences(revision, repositoryName).toArray(new Revision[1])[0]);
    }
    return null;
  }
}
