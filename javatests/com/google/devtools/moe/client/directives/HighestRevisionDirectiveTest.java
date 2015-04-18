// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Moe;
import com.google.devtools.moe.client.NullFileSystemModule;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;
import com.google.devtools.moe.client.testing.TestingModule;

import junit.framework.TestCase;

import javax.inject.Singleton;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class HighestRevisionDirectiveTest extends TestCase {
  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {
      TestingModule.class,
      SystemCommandRunner.Module.class,
      NullFileSystemModule.class})
  @Singleton
  interface Component extends Moe.Component {
    @Override Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = DaggerCreateCodebaseDirectiveTest_Component.create().context();
  }

  public void testWithoutRevision() throws Exception {
    ((InMemoryProjectContextFactory) Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    HighestRevisionDirective d = new HighestRevisionDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repository = "internal";
    assertEquals(0, d.perform());
    assertEquals(
        "Highest revision in repository \"internal\": 1",
        ((RecordingUi) Injector.INSTANCE.ui).lastInfo);
  }

  public void testWithRevision() throws Exception {
    ((InMemoryProjectContextFactory) Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    HighestRevisionDirective d = new HighestRevisionDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repository = "internal(revision=4)";
    assertEquals(0, d.perform());
    assertEquals(
        "Highest revision in repository \"internal\": 4",
        ((RecordingUi) Injector.INSTANCE.ui).lastInfo);
  }
}
