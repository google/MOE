// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

/**
 * Tests for RevisionMetadata operations.
 *
 */
public class RevisionMetadataTest extends TestCase {

  public void testConcatenate_singleMetadata() {
    RevisionMetadata rm = new RevisionMetadata(
        "id", "auth", "date", "description", ImmutableList.of(new Revision("revId", "repo")));

    assertEquals(rm, RevisionMetadata.concatenate(ImmutableList.of(rm), null));
    assertEquals(rm.toString(),
                 RevisionMetadata.concatenate(ImmutableList.of(rm), null).toString());
  }

  public void testConcatenate_twoMetadata() {
    RevisionMetadata rm1 = new RevisionMetadata(
        "id1", "auth1", "date1", "description1", ImmutableList.of(new Revision("revId1", "repo")));
    RevisionMetadata rm2 = new RevisionMetadata(
        "id2", "auth2", "date2", "description2", ImmutableList.of(new Revision("revId2", "repo")));

    RevisionMetadata rmExpected = new RevisionMetadata(
        "id1, id2",
        "auth1, auth2",
        "date1, date2",
        "description1\n-------------\ndescription2",
        ImmutableList.of(new Revision("revId1", "repo"), new Revision("revId2", "repo")));

    assertEquals(rmExpected, RevisionMetadata.concatenate(ImmutableList.of(rm1, rm2), null));
    assertEquals(rmExpected.toString(),
                 RevisionMetadata.concatenate(ImmutableList.of(rm1, rm2), null).toString());
  }

  public void testConcatenate_withMigrationInfo() {
    RevisionMetadata rm1 = new RevisionMetadata(
        "id1", "auth1", "date1", "description1", ImmutableList.of(new Revision("revId1", "repo")));
    RevisionMetadata rm2 = new RevisionMetadata(
        "id2", "auth2", "date2", "description2", ImmutableList.of(new Revision("revId2", "repo")));
    Revision migrationFromRev = new Revision("migrationRevId", "repo");

    RevisionMetadata rmExpected = new RevisionMetadata(
        "id1, id2",
        "auth1, auth2",
        "date1, date2",
        "description1\n-------------\ndescription2"
            + "\n-------------\nCreated by MOE: http://code.google.com/p/moe-java\n"
            + "MOE_MIGRATED_REVID=" + migrationFromRev.revId,
        ImmutableList.of(new Revision("revId1", "repo"), new Revision("revId2", "repo")));

    assertEquals(rmExpected,
                 RevisionMetadata.concatenate(ImmutableList.of(rm1, rm2), migrationFromRev));
    assertEquals(rmExpected.toString(),
                 RevisionMetadata.concatenate(ImmutableList.of(rm1, rm2), migrationFromRev)
                     .toString());

  }
}
