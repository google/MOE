// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.joda.time.DateTime;

/**
 * Testing for DescriptionMetadataScrubber (changelog transformations).
 *
 */
public class DescriptionMetadataScrubberTest extends TestCase {

  public void testScrub() {
    RevisionMetadata rm =
        new RevisionMetadata(
            "commit_number",
            "author@google.com",
            new DateTime(1L),
            "some changes",
            ImmutableList.of(
                Revision.create("parentId1", "repo"), Revision.create("parentId2", "repo")));

    // Test that fields besides description are unaffected.
    RevisionMetadata rmExpected =
        new RevisionMetadata(
            "commit_number",
            "author@google.com",
            new DateTime(1L),
            "some changes!!!",
            ImmutableList.of(
                Revision.create("parentId1", "repo"), Revision.create("parentId2", "repo")));
    assertEquals(rmExpected, new DescriptionMetadataScrubber("{description}!!!").scrub(rm));

    // Test various formats.
    assertEquals(
        "some changes on 1969/12/31",
        new DescriptionMetadataScrubber("{description} on {date}").scrub(rm).description);

    assertEquals(
        "some changes on 1969/12/31\nby: author@google.com",
        new DescriptionMetadataScrubber("{description} on {date}\nby: {author}")
            .scrub(rm).description);

    assertEquals(
        "1969/12/31 saw the birth of commit_number",
        new DescriptionMetadataScrubber("{date} saw the birth of {id}").scrub(rm).description);

    assertEquals(
        "my parents are (parentId1, parentId2)",
        new DescriptionMetadataScrubber("my parents are ({parents})").scrub(rm).description);
  }
}
