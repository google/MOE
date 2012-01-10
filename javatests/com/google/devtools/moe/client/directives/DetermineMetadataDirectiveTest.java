// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 * Test to ensure the DetermineMetadataDirective produces the expected output.
 *
 */
public class DetermineMetadataDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  /**
   *  When two or more revisions are given, the metadata fields are concatenated.
   */
  public void testDetermineMetadata() throws Exception {
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    DetermineMetadataDirective d = new DetermineMetadataDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repositoryExpression = "internal(revision=\"1,2\")";
    assertEquals(0, d.perform());
    RevisionMetadata rm = new RevisionMetadata("1, 2", "author, author", "date, date",
        "description\n-------------\ndescription",
        ImmutableList.of(new Revision("parent", "internal"),
        new Revision("parent", "internal")));
    assertEquals(rm.toString(), ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }

  /**
   *  When only one revision is given, the new metadata should be identical to
   *  that revision's metadata.
   */
  public void testDetermineMetadataOneRevision() throws Exception {
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    DetermineMetadataDirective d = new DetermineMetadataDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repositoryExpression = "internal(revision=7)";
    assertEquals(0, d.perform());
    RevisionMetadata rm = new RevisionMetadata("7", "author", "date", "description",
        ImmutableList.of(new Revision("parent", "internal")));
    assertEquals(rm.toString(), ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }
}
