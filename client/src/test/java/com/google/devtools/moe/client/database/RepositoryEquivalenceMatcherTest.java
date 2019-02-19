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
import com.google.devtools.moe.client.GsonModule;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionGraph;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import junit.framework.TestCase;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Unit tests for EquivalenceMatcher
 */
public class RepositoryEquivalenceMatcherTest extends TestCase {
  /*
   * A database that holds the following equivalences:
   * repo1{1001} == repo2{1}
   * repo1{1002} == repo2{2}
   */
  private final String testDb1 =
      "{\"equivalences\":["
          + "{\"rev1\": {\"revId\":\"1001\",\"repositoryName\":\"repo1\"},"
          + " \"rev2\": {\"revId\":\"1\",\"repositoryName\":\"repo2\"}},"
          + "{\"rev1\": {\"revId\":\"1002\",\"repositoryName\":\"repo1\"},"
          + " \"rev2\": {\"revId\":\"2\",\"repositoryName\":\"repo2\"}}]}";

  private FileDb database;
  private RepositoryEquivalenceMatcher matcher;

  @Override
  public void setUp() {
    try {
      DbStorage dbStorage = GsonModule.provideGson().fromJson(testDb1, DbStorage.class);
      database = new FileDb(null, dbStorage, null);
    } catch (InvalidProject e) {
      e.printStackTrace();
      throw e;
    }
    matcher = new RepositoryEquivalenceMatcher("repo2", database);
  }

  public void testMatches() throws Exception {
    assertTrue(matcher.matches(Revision.create(1001, "repo1")));
    assertTrue(matcher.matches(Revision.create(1002, "repo1")));
    assertFalse(matcher.matches(Revision.create(1003, "repo1")));
  }

  public void testMakeResult() throws Exception {
    Revision startingRev = Revision.create(1003, "repo1");
    List<Revision> matching = ImmutableList.of(Revision.create(1002, "repo1"));
    RevisionGraph nonMatching =
        RevisionGraph.builder(matching)
            .addRevision(
                startingRev,
                RevisionMetadata.builder()
                    .id("id")
                    .author("author")
                    .date(DateTime.now())
                    .description("desc")
                    .withParents(matching)
                    .build())
            .build();

    RepositoryEquivalenceMatcher.Result result = matcher.makeResult(nonMatching, matching);
    assertEquals(nonMatching, result.getRevisionsSinceEquivalence());

    RepositoryEquivalence expectedEquiv =
        RepositoryEquivalence.create(
            Revision.create(2, "repo2"), Revision.create(1002, "repo1"));
    assertEquals(expectedEquiv, result.getEquivalences().get(0));
  }
}
