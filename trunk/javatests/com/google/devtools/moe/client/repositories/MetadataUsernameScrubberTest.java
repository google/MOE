// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

/**
 * Unit test for the MetadataUsernameScrubber.
 *
 */
public class MetadataUsernameScrubberTest extends TestCase {

  public void testScrubOnOneUser() throws Exception {
    RevisionMetadata before = new RevisionMetadata("100", "Bob Saget",
        "Yesterday", "saget fixed ALL the bugs.", null);
    MetadataUsernameScrubber mus =
        new MetadataUsernameScrubber(ImmutableList.of("saget"));
    RevisionMetadata expected = new RevisionMetadata("100", "Bob <user>",
        "Yesterday", "<user> fixed ALL the bugs.", null);
    RevisionMetadata after = mus.scrub(before);
    assertEquals(expected, after);
  }

  public void testScrubOnTwoUsers() throws Exception {
    RevisionMetadata before = new RevisionMetadata("100", "Bob, Saget",
        "Yesterday", "bob and saget fixed ALL the bugs.", null);
    MetadataUsernameScrubber mus =
        new MetadataUsernameScrubber(ImmutableList.of("bob", "saget"));
    RevisionMetadata expected = new RevisionMetadata("100", "<user>, <user>",
        "Yesterday", "<user> and <user> fixed ALL the bugs.", null);
    RevisionMetadata after = mus.scrub(before);
    assertEquals(expected, after);
  }

  public void testFalsePositive() throws Exception {
    RevisionMetadata before = new RevisionMetadata("100", "Bob, Saget",
        "Yesterday", "bob and \nsaget went bobbing for apples in Sagetopolis.", null);
    MetadataUsernameScrubber mus =
        new MetadataUsernameScrubber(ImmutableList.of("bob", "saget"));
    RevisionMetadata expected = new RevisionMetadata("100", "<user>, <user>",
        "Yesterday", "<user> and \n<user> went bobbing for apples in Sagetopolis.", null);
    RevisionMetadata after = mus.scrub(before);
    assertEquals(expected, after);
  }
}
