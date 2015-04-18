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
public class CreateCodebaseDirectiveTest extends TestCase {
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

  public void testCreateCodebase() throws Exception {
    ((InMemoryProjectContextFactory)Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    CreateCodebaseDirective d = new CreateCodebaseDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().codebase = "internal";
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Codebase \"%s\" created at %s", "internal", "/dummy/codebase/internal/1"),
        ((RecordingUi)Injector.INSTANCE.ui).lastInfo);
  }

  public void testCreateCodebaseWithEditors() throws Exception {
    ((InMemoryProjectContextFactory)Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}, \"editors\": {" +
        "\"identity\": {\"type\":\"identity\"}}}");
    CreateCodebaseDirective d = new CreateCodebaseDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().codebase = "internal|identity";
    assertEquals(0, d.perform());
    assertEquals(
        String.format(
            "Codebase \"%s\" created at %s", "internal|identity", "/dummy/codebase/internal/1"),
        ((RecordingUi)Injector.INSTANCE.ui).lastInfo);
  }
}
