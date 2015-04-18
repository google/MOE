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
 * Tests for {@link ChangeDirective}.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ChangeDirectiveTest extends TestCase {
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
    Injector.INSTANCE = DaggerChangeDirectiveTest_Component.create().context();
  }

  public void testChange() throws Exception {
    ((InMemoryProjectContextFactory)Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    ChangeDirective d = new ChangeDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().codebase = "internal";
    d.getFlags().destination = "internal";
    assertEquals(0, d.perform());
    assertEquals("/dummy/writer/internal", ((RecordingUi) Injector.INSTANCE.ui).lastTaskResult);
  }
}
