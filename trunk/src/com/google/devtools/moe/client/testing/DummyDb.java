// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.repositories.Revision;

import java.util.Set;

/**
 * Db for testing
 *
 */
public class DummyDb implements Db {

  public DummyDb() {}

  public void noteEquivalence(Equivalence equivalence) {}

  public Set<Revision> findEquivalences(Revision revision, String otherRepository) {
    return ImmutableSet.of(new Revision("1", otherRepository),
                           new Revision("2", otherRepository));
  }
}
