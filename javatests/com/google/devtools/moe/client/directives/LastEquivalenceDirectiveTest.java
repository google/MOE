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
 * Unit test for LastEquivalenceDirective.
 *
 */
public class LastEquivalenceDirectiveTest extends TestCase {
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

  public void testPerform() throws Exception {
    ((InMemoryProjectContextFactory) Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    LastEquivalenceDirective d = new LastEquivalenceDirective();
    LastEquivalenceDirective.LastEquivalenceOptions options =
        ((LastEquivalenceDirective.LastEquivalenceOptions) d.getFlags());
    options.configFilename = "moe_config.txt";
    options.dbLocation = "dummy";
    options.fromRepository = "internal(revision=1)";
    options.withRepository = "public";
    assertEquals(0, d.perform());
    assertEquals("Last equivalence: internal{1} == public{1}",
                 ((RecordingUi) Injector.INSTANCE.ui).lastInfo);
  }
}
