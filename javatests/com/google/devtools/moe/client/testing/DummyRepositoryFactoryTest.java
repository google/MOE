// Copyright 2012 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory.DummyCommit;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory.DummyRevisionHistory;

import junit.framework.TestCase;

import org.joda.time.DateTime;

/**
 * Unit tests for {@link DummyRepositoryFactory}.
 */
public class DummyRepositoryFactoryTest extends TestCase {
  private static final long HOUR = 360000; // in milliseconds.

  public void testCommmitsBecomeRevisions() throws Exception {
    DummyCommit c1 = DummyCommit.create("1", "foo@foo.bar", "rev 1", new DateTime(1 * HOUR));
    DummyCommit c2 = DummyCommit.create("2", "bar@foo.bar", "rev 2", new DateTime(2 * HOUR), c1);
    DummyRevisionHistory history = new DummyRevisionHistory("foo", c1, c2);
    assertThat(history.findHighestRevision(null)).isEqualTo(Revision.create("2", "foo"));
    assertThat(history.findHighestRevision("2")).isEqualTo(Revision.create("2", "foo"));
    assertThat(history.findHighestRevision("1")).isEqualTo(Revision.create("1", "foo"));
  }

  public void testMetadataCreatedFromCommits() throws Exception {
    DummyCommit c1 = DummyCommit.create("1", "foo@foo.bar", "rev 1", new DateTime(1 * HOUR));
    DummyCommit c2 = DummyCommit.create("2", "bar@foo.bar", "rev 2", new DateTime(2 * HOUR), c1);
    DummyRevisionHistory history = new DummyRevisionHistory("foo", c1, c2);

    Revision head = history.findHighestRevision(null);
    assertThat(head).isEqualTo(Revision.create("2", "foo")); // just to make sure.

    RevisionMetadata metadata = history.getMetadata(head);
    assertThat(metadata.id).isEqualTo("2");
    assertThat(metadata.author).isEqualTo("bar@foo.bar");
    assertThat(metadata.date).isEqualTo(new DateTime(2 * HOUR));
    assertThat(metadata.description).isEqualTo("rev 2");
    assertThat(metadata.parents).containsExactly(Revision.create("1", "foo"));

    metadata = history.getMetadata(Revision.create("1", "foo"));
    assertThat(metadata.id).isEqualTo("1");
    assertThat(metadata.author).isEqualTo("foo@foo.bar");
    assertThat(metadata.date).isEqualTo(new DateTime(1 * HOUR));
    assertThat(metadata.description).isEqualTo("rev 1");
    assertThat(metadata.parents).isEmpty();
  }

  public void testLenientFindHeadInEmptyHistoryReturnsCannedDefault() {
    DummyRevisionHistory history = new DummyRevisionHistory("foo", true);
    assertThat(history.findHighestRevision(null)).isEqualTo(Revision.create("1", "foo"));
  }

  public void testStrictFindHeadInEmptyHistoryReturnsNull() {
    DummyRevisionHistory history = new DummyRevisionHistory("foo", false);
    assertThat(history.findHighestRevision(null)).isNull();
  }

  public void testLenientFindNonExistentRevisionReturnsAskedForRevision() {
    DummyRevisionHistory history = new DummyRevisionHistory("foo", true);
    assertThat(history.findHighestRevision("5")).isEqualTo(Revision.create("5", "foo"));
  }

  public void testStrictFindNonExistentRevisionReturnsNull() {
    DummyRevisionHistory history = new DummyRevisionHistory("foo", false);
    assertThat(history.findHighestRevision("5")).isNull();
  }

  public void testDefaultGetMetadataReturnsCannedData() {
    // default preserved for backward compatibility with older tests.
    testGetMetadataReturnsCannedData(new DummyRevisionHistory("foo" /* , true */));
  }

  public void testLenientGetMetadataReturnsCannedData() {
    // lenient mode for backward compatibility with older tests.
    testGetMetadataReturnsCannedData(new DummyRevisionHistory("foo", true));
  }

  private void testGetMetadataReturnsCannedData(RevisionHistory history) {
    RevisionMetadata metadata = history.getMetadata(Revision.create("1", "foo"));
    assertThat(metadata).isNotNull();
    assertThat(metadata.id).isEqualTo("1");
    assertThat(metadata.author).isEqualTo("author");
    assertThat(metadata.date).isEqualTo(new DateTime(1L));
    assertThat(metadata.description).isEqualTo("description");
    assertThat(metadata.parents).containsExactly(Revision.create("parent", "foo"));
  }

  public void testStrictGetMetadataReturnsNull() {
    DummyRevisionHistory history = new DummyRevisionHistory("foo", false);
    RevisionMetadata metadata = history.getMetadata(Revision.create("1", "foo"));
    assertThat(metadata).isNull();
  }
}
