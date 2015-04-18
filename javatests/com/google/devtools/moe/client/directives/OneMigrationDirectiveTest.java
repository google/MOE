// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Moe;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.NullFileSystemModule;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import dagger.Provides;

import junit.framework.TestCase;

import javax.inject.Singleton;

/**
 */
public class OneMigrationDirectiveTest extends TestCase {
  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {
      RecordingUi.Module.class,
      SystemCommandRunner.Module.class,
      NullFileSystemModule.class,
      Module.class})
  @Singleton
  interface Component extends Moe.Component {
    @Override Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module class Module {
    @Provides public ProjectContextFactory projectContextFactory() {
      InMemoryProjectContextFactory contextFactory = new InMemoryProjectContextFactory();
      contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\":\"foo\",\"repositories\":{" +
        "\"int\":{\"type\":\"dummy\",\"project_space\":\"internal\"}," +
        "\"pub\":{\"type\":\"dummy\"}}," +
        "\"translators\":[{\"from_project_space\":\"internal\"," +
        "\"to_project_space\":\"public\",\"steps\":[{\"name\":\"id_step\"," +
        "\"editor\":{\"type\":\"identity\"}}]}]}");
      return contextFactory;
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = DaggerOneMigrationDirectiveTest_Component.builder().module(new Module())
        .build().context();
  }

  public void testOneMigration() throws Exception {
    OneMigrationDirective d = new OneMigrationDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().fromRepository = "int(revision=1000)";
    d.getFlags().toRepository = "pub(revision=2)";
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Created Draft Revision: %s", "/dummy/revision/pub"),
        ((RecordingUi) Injector.INSTANCE.ui).lastInfo);
  }

  public void testOneMigrationFailOnFromRevision() throws Exception {
    OneMigrationDirective d = new OneMigrationDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().fromRepository = "x(revision=1000)";
    d.getFlags().toRepository = "pub(revision=2)";
    try {
      d.perform();
      fail("OneMigrationDirective didn't fail on invalid repository 'x'.");
    } catch (MoeProblem expected) {
      assertEquals(
          "No such repository 'x' in the config. Found: [int, pub]",
          expected.getMessage());
    }
  }

  public void testOneMigrationFailOnToRevision() throws Exception {
    OneMigrationDirective d = new OneMigrationDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().fromRepository = "int(revision=1000)";
    d.getFlags().toRepository = "x(revision=2)";
    try {
      int result = d.perform();
      fail("OneMigrationDirective didn't fail on invalid repository 'x'.");
    } catch (MoeProblem expected) {
      assertEquals(
          "No such repository 'x' in the config. Found: [int, pub]",
          expected.getMessage());
    }
  }
}
