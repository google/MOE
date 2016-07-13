/*
 * Copyright (c) 2012 Google, Inc.
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

package com.google.devtools.moe.client.testing;

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.junit.Assert;

/**
 * Unit tests for {@link DummyRepositoryFactory}.
 */
public class DummyRevisionHistoryTest extends TestCase {
  private static final long HOUR = 360000; // in milliseconds.

  public void testCommmitsBecomeRevisions() throws Exception {
    DummyCommit c1 = DummyCommit.create("1", "foo@foo.bar", "rev 1", new DateTime(1 * HOUR));
    DummyCommit c2 = DummyCommit.create("2", "bar@foo.bar", "rev 2", new DateTime(2 * HOUR), c1);
    DummyRevisionHistory history = DummyRevisionHistory.builder().name("foo").add(c1, c2).build();
    assertThat(history.findHighestRevision(null)).isEqualTo(Revision.create("2", "foo"));
    assertThat(history.findHighestRevision("2")).isEqualTo(Revision.create("2", "foo"));
    assertThat(history.findHighestRevision("1")).isEqualTo(Revision.create("1", "foo"));
  }

  public void testUniqueRevisionIds() throws Exception {
    DummyCommit c1 = DummyCommit.create("1", "foo@foo.bar", "rev 1", new DateTime(1 * HOUR));
    DummyCommit c2 = DummyCommit.create("1", "bar@foo.bar", "rev 2", new DateTime(2 * HOUR), c1);
    try {
      DummyRevisionHistory.builder().name("foo").add(c1, c2);
      Assert.fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected)
          .hasMessage("A commit with id '1' has already been registered in this history.");
    }
  }

  public void testMetadataCreatedFromCommits() throws Exception {
    DummyCommit c1 = DummyCommit.create("1", "foo@foo.bar", "rev 1", new DateTime(1 * HOUR));
    DummyCommit c2 = DummyCommit.create("2", "bar@foo.bar", "rev 2", new DateTime(2 * HOUR), c1);
    DummyRevisionHistory history = DummyRevisionHistory.builder().name("foo").add(c1, c2).build();

    Revision head = history.findHighestRevision(null);
    assertThat(head).isEqualTo(Revision.create("2", "foo")); // just to make sure.

    RevisionMetadata metadata = history.getMetadata(head);
    assertThat(metadata.id()).isEqualTo("2");
    assertThat(metadata.author()).isEqualTo("bar@foo.bar");
    assertThat(metadata.date()).isEqualTo(new DateTime(2 * HOUR));
    assertThat(metadata.description()).isEqualTo("rev 2");
    assertThat(metadata.parents()).containsExactly(Revision.create("1", "foo"));

    metadata = history.getMetadata(Revision.create("1", "foo"));
    assertThat(metadata.id()).isEqualTo("1");
    assertThat(metadata.author()).isEqualTo("foo@foo.bar");
    assertThat(metadata.date()).isEqualTo(new DateTime(1 * HOUR));
    assertThat(metadata.description()).isEqualTo("rev 1");
    assertThat(metadata.parents()).isEmpty();
  }

  public void testLenientFindHeadInEmptyHistoryReturnsCannedDefault() {
    DummyRevisionHistory history =
        DummyRevisionHistory.builder().name("foo").permissive(true).build();
    assertThat(history.findHighestRevision(null)).isEqualTo(Revision.create("1", "foo"));
  }

  public void testStrictFindHeadInEmptyHistoryReturnsNull() {
    DummyRevisionHistory history =
        DummyRevisionHistory.builder().name("foo").permissive(false).build();
    assertThat(history.findHighestRevision(null)).isNull();
  }

  public void testLenientFindNonExistentRevisionReturnsAskedForRevision() {
    DummyRevisionHistory history =
        DummyRevisionHistory.builder().name("foo").permissive(true).build();
    assertThat(history.findHighestRevision("5")).isEqualTo(Revision.create("5", "foo"));
  }

  public void testStrictFindNonExistentRevisionReturnsNull() {
    DummyRevisionHistory history =
        DummyRevisionHistory.builder().name("foo").permissive(false).build();
    assertThat(history.findHighestRevision("5")).isNull();
  }

  public void testDefaultGetMetadataReturnsCannedData() {
    // default permissibility preserved for backward compatibility with older tests.
    testGetMetadataReturnsCannedData(DummyRevisionHistory.builder().name("foo").build());
  }

  public void testLenientGetMetadataReturnsCannedData() {
    // lenient mode for backward compatibility with older tests.
    testGetMetadataReturnsCannedData(
        DummyRevisionHistory.builder().name("foo").permissive(true).build());
  }

  private void testGetMetadataReturnsCannedData(RevisionHistory history) {
    RevisionMetadata metadata = history.getMetadata(Revision.create("1", "foo"));
    assertThat(metadata).isNotNull();
    assertThat(metadata.id()).isEqualTo("1");
    assertThat(metadata.author()).isEqualTo("author");
    assertThat(metadata.date()).isEqualTo(new DateTime(1L));
    assertThat(metadata.description()).isEqualTo("description");
    assertThat(metadata.parents()).containsExactly(Revision.create("parent", "foo"));
  }

  public void testStrictGetMetadataReturnsNull() {
    DummyRevisionHistory history =
        DummyRevisionHistory.builder().name("foo").permissive(false).build();
    RevisionMetadata metadata = history.getMetadata(Revision.create("1", "foo"));
    assertThat(metadata).isNull();
  }

  public void testCommmitsCanBeRegistered() throws Exception {
    DummyRevisionHistory history =
        DummyRevisionHistory.builder()
            .name("foo")
            .permissive(false)
            .add("1", "foo@foo.bar", "rev 1", new DateTime(1 * HOUR))
            .add("2", "bar@foo.bar", "rev 2", new DateTime(2 * HOUR), "1")
            .build();
    assertThat(history.findHighestRevision(null)).isEqualTo(Revision.create("2", "foo"));
    assertThat(history.findHighestRevision("2")).isEqualTo(Revision.create("2", "foo"));
    assertThat(history.findHighestRevision("1")).isEqualTo(Revision.create("1", "foo"));
  }

  public void testRegisteredCommitParentIsAlreadyRegistered() throws Exception {
    try {
      DummyRevisionHistory.builder()
          .name("foo")
          .permissive(false)
          .add("2", "bar@foo.bar", "rev 2", new DateTime(2 * HOUR), "1")
          .build();
      Assert.fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
