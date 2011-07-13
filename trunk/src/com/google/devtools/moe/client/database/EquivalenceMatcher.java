// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMatcher;

/**
 * RevisionMatcher checking the property of the revision having equivalences in the database
 * matching the repository name
 *
 */
public class EquivalenceMatcher implements RevisionMatcher {

  private final String repositoryName;
  private final Db db;

  public EquivalenceMatcher(String repositoryName, Db db) {
    this.repositoryName = repositoryName;
    this.db = db;
  }

  public boolean matches(Revision revision) {
    return !db.findEquivalences(revision, repositoryName).isEmpty();
  }

}
