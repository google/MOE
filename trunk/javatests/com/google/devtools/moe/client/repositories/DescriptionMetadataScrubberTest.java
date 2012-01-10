// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

/**
 * Testing for DescriptionMetadataScrubber (changelog transformations).
 *
 */
public class DescriptionMetadataScrubberTest extends TestCase {

  public void testScrub() {
    RevisionMetadata rm = new RevisionMetadata(
        "commit_number",
        "author@google.com",
        "3/7/2001",
        "some changes",
        ImmutableList.of(new Revision("parentId1", "repo"), new Revision("parentId2", "repo")));

    // Test that fields besides description are unaffected.
    RevisionMetadata rmExpected = new RevisionMetadata(
        "commit_number",
        "author@google.com",
        "3/7/2001",
        "some changes!!!",
        ImmutableList.of(new Revision("parentId1", "repo"), new Revision("parentId2", "repo")));
    assertEquals(rmExpected, new DescriptionMetadataScrubber("{description}!!!").scrub(rm));

    // Test various formats.
    assertEquals(
        "some changes on 3/7/2001",
        new DescriptionMetadataScrubber("{description} on {date}").scrub(rm).description);

    assertEquals(
        "some changes on 3/7/2001\nby: author@google.com",
        new DescriptionMetadataScrubber("{description} on {date}\nby: {author}")
            .scrub(rm).description);

    assertEquals(
        "3/7/2001 saw the birth of commit_number",
        new DescriptionMetadataScrubber("{date} saw the birth of {id}").scrub(rm).description);

    assertEquals(
        "my parents are (parentId1, parentId2)",
        new DescriptionMetadataScrubber("my parents are ({parents})").scrub(rm).description);
  }
}
