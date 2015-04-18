// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Moe;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.SystemFileSystem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;
import com.google.devtools.moe.client.testing.TestingModule;

import junit.framework.TestCase;

import org.joda.time.DateTime;

import javax.inject.Singleton;

/**
 * Test to ensure the DetermineMetadataDirective produces the expected output.
 *
 */
public class DetermineMetadataDirectiveTest extends TestCase {
  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {
      TestingModule.class,
      SystemCommandRunner.Module.class,
      SystemFileSystem.Module.class})
  @Singleton
  interface Component extends Moe.Component {
    @Override Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = DaggerDetermineMetadataDirectiveTest_Component.create().context();
  }

  /**
   *  When two or more revisions are given, the metadata fields are concatenated.
   */
  public void testDetermineMetadata() throws Exception {
    ((InMemoryProjectContextFactory) Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    DetermineMetadataDirective d = new DetermineMetadataDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repositoryExpression = "internal(revision=\"1,2\")";
    assertEquals(0, d.perform());
    RevisionMetadata rm = new RevisionMetadata("1, 2", "author, author",
        new DateTime(1L),
        "description\n-------------\ndescription",
        ImmutableList.of(new Revision("parent", "internal"),
        new Revision("parent", "internal")));
    assertEquals(rm.toString(), ((RecordingUi) Injector.INSTANCE.ui).lastInfo);
  }

  /**
   *  When only one revision is given, the new metadata should be identical to
   *  that revision's metadata.
   */
  public void testDetermineMetadataOneRevision() throws Exception {
    ((InMemoryProjectContextFactory) Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    DetermineMetadataDirective d = new DetermineMetadataDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repositoryExpression = "internal(revision=7)";
    assertEquals(0, d.perform());
    RevisionMetadata rm = new RevisionMetadata("7", "author",
        new DateTime(1L), "description",
        ImmutableList.of(new Revision("parent", "internal")));
    assertEquals(rm.toString(), ((RecordingUi) Injector.INSTANCE.ui).lastInfo);
  }
}
