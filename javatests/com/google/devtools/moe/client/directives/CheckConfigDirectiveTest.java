// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Moe;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.SystemFileSystem;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.TestingModule;

import junit.framework.TestCase;

import javax.inject.Singleton;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CheckConfigDirectiveTest extends TestCase {
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
    Injector.INSTANCE = DaggerCheckConfigDirectiveTest_Component.create().context();
  }

  public void testEmptyConfigFilenameReturnsOne() throws Exception {
    ((InMemoryProjectContextFactory)Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "");
    CheckConfigDirective d = new CheckConfigDirective();
    assertEquals(1, d.perform());
  }

  public void testEmptyConfigFileReturnsOne() throws Exception {
    ((InMemoryProjectContextFactory)Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "");
    CheckConfigDirective d = new CheckConfigDirective();
    d.getFlags().configFilename = "moe_config.txt";
    assertEquals(1, d.perform());
  }

  public void testSimpleConfigFileWorks() throws Exception {
    ((InMemoryProjectContextFactory)Injector.INSTANCE.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": " +
        "{\"public\": {\"type\": \"dummy\"}}}");
    CheckConfigDirective d = new CheckConfigDirective();
    d.getFlags().configFilename = "moe_config.txt";
    assertEquals(0, d.perform());
  }

}
