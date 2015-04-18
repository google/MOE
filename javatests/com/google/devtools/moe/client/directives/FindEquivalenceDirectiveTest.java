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
 */
public class FindEquivalenceDirectiveTest extends TestCase {
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

  public void testFindEquivalenceDirective() throws Exception {
    ((InMemoryProjectContextFactory) Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"test\",\"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    FindEquivalenceDirective d = new FindEquivalenceDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = "dummy";
    d.getFlags().fromRepository = "internal(revision=1)";
    d.getFlags().inRepository = "public";
    assertEquals(0, d.perform());
    assertEquals(
        "\"internal{1}\" == \"public{1,2}\"",
        ((RecordingUi) Injector.INSTANCE.ui).lastInfo);
  }
}
