// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.joda.time.DateTime;

/**
 * Tests for PublicSectionMetadataScrubber.
 *
 */
public class PublicSectionMetadataScrubberTest extends TestCase {

  private static RevisionMetadata makeWithDescription(String... desc) {
    return new RevisionMetadata(
        "commit_number",
        "author@google.com",
        new DateTime(1L),
        Joiner.on("\n").join(desc),
        ImmutableList.of(new Revision("parentId1", "repo"), new Revision("parentId2", "repo")));
  }

  public void testScrub() {
    RevisionMetadata rm =
        makeWithDescription(
            "Top secret stuff.",
            "",
            "Public:  ",
            "some changes",
            "intended for public use",
            "",
            "unrelated footer");

    // Test that fields besides description are unaffected.
    RevisionMetadata rmExpected = makeWithDescription("some changes", "intended for public use");
    assertEquals(rmExpected, new PublicSectionMetadataScrubber().scrub(rm));

    // Test various Strings.
    assertEquals(
        Joiner.on("\n").join("whitespace", "okay"),
        new PublicSectionMetadataScrubber()
            .scrub(makeWithDescription("Internal", "  Public: \t", "whitespace", "okay"))
            .description);

    assertEquals(
        Joiner.on("\n").join("no", "change"),
        new PublicSectionMetadataScrubber().scrub(makeWithDescription("no", "change")).description);

    assertEquals(
        "",
        new PublicSectionMetadataScrubber()
            .scrub(makeWithDescription("Internal", "Public:")).description);

    assertEquals(
        Joiner.on("\n").join("section", "first"),
        new PublicSectionMetadataScrubber()
            .scrub(makeWithDescription("Public:", "section", "first", "", "Then internal desc"))
            .description);

    assertEquals(
        "then public",
        new PublicSectionMetadataScrubber()
            .scrub(makeWithDescription("Unrelated:", "section", "Public:", "then public"))
            .description);

    assertEquals(
        "last public section",
        new PublicSectionMetadataScrubber()
            .scrub(
                makeWithDescription(
                    "Public:", "first public section", "", "Public:", "last public section"))
            .description);
  }
}
