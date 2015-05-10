// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.database.EquivalenceMatcher.EquivalenceMatchResult;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionGraph;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import junit.framework.TestCase;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Unit tests for EquivalenceMatcher.
 *
 */
public class EquivalenceMatcherTest extends TestCase {

  /*
   * A database that holds the following equivalences:
   * repo1{1001} == repo2{1}
   * repo1{1002} == repo2{2}
   */
  private final String testDb1 = "{\"equivalences\":["
      + "{\"rev1\": {\"revId\":\"1001\",\"repositoryName\":\"repo1\"},"
      + " \"rev2\": {\"revId\":\"1\",\"repositoryName\":\"repo2\"}},"
      + "{\"rev1\": {\"revId\":\"1002\",\"repositoryName\":\"repo1\"},"
      + " \"rev2\": {\"revId\":\"2\",\"repositoryName\":\"repo2\"}}]}";

  private FileDb database;
  private EquivalenceMatcher equivalenceMatcher;

  @Override
  public void setUp() {
    try {
      database = FileDb.makeDbFromDbText(testDb1);
    } catch (InvalidProject e) {
      e.printStackTrace();
    }
    equivalenceMatcher = new EquivalenceMatcher("repo2", database);
  }

  public void testMatches() throws Exception {
    assertTrue(equivalenceMatcher.matches(new Revision("1001", "repo1")));
    assertTrue(equivalenceMatcher.matches(new Revision("1002", "repo1")));
    assertFalse(equivalenceMatcher.matches(new Revision("1003", "repo1")));
  }

  public void testMakeResult() throws Exception {
    Revision startingRev = new Revision("1003", "repo1");
    List<Revision> matching = ImmutableList.of(new Revision("1002", "repo1"));
    RevisionGraph nonMatching = RevisionGraph.builder(matching)
        .addRevision(
            startingRev,
            new RevisionMetadata("id", "author", DateTime.now(), "desc", matching))
        .build();

    EquivalenceMatchResult result = equivalenceMatcher.makeResult(nonMatching, matching);
    assertEquals(nonMatching, result.getRevisionsSinceEquivalence());

    Equivalence expectedEquiv =
        new Equivalence(new Revision("2", "repo2"), new Revision("1002", "repo1"));
    assertEquals(expectedEquiv, result.getEquivalences().get(0));
  }
}
