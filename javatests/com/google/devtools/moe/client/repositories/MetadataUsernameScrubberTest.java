// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.joda.time.DateTime;

/**
 * Unit test for the MetadataUsernameScrubber.
 *
 */
public class MetadataUsernameScrubberTest extends TestCase {

  public void testScrubOnOneUser() throws Exception {
    RevisionMetadata before =
        new RevisionMetadata(
            "100", "Bob Saget", new DateTime(1L), "saget fixed ALL the bugs.", null);
    MetadataUsernameScrubber mus = new MetadataUsernameScrubber(ImmutableList.of("saget"));
    RevisionMetadata expected =
        new RevisionMetadata(
            "100", "Bob <user>", new DateTime(1L), "<user> fixed ALL the bugs.", null);
    RevisionMetadata after = mus.scrub(before);
    assertEquals(expected, after);
  }

  public void testScrubOnTwoUsers() throws Exception {
    RevisionMetadata before =
        new RevisionMetadata(
            "100", "Bob, Saget", new DateTime(1L), "bob and saget fixed ALL the bugs.", null);
    MetadataUsernameScrubber mus = new MetadataUsernameScrubber(ImmutableList.of("bob", "saget"));
    RevisionMetadata expected =
        new RevisionMetadata(
            "100",
            "<user>, <user>",
            new DateTime(1L),
            "<user> and <user> fixed ALL the bugs.",
            null);
    RevisionMetadata after = mus.scrub(before);
    assertEquals(expected, after);
  }

  public void testFalsePositive() throws Exception {
    RevisionMetadata before =
        new RevisionMetadata(
            "100",
            "Bob, Saget",
            new DateTime(1L),
            "bob and \nsaget went bobbing for apples in Sagetopolis.",
            null);
    MetadataUsernameScrubber mus = new MetadataUsernameScrubber(ImmutableList.of("bob", "saget"));
    RevisionMetadata expected =
        new RevisionMetadata(
            "100",
            "<user>, <user>",
            new DateTime(1L),
            "<user> and \n<user> went bobbing for apples in Sagetopolis.",
            null);
    RevisionMetadata after = mus.scrub(before);
    assertEquals(expected, after);
  }
}
