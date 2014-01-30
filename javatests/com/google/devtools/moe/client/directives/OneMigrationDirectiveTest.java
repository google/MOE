// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeModule;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import junit.framework.TestCase;

/**
 */
public class OneMigrationDirectiveTest extends TestCase {
  @Module(overrides = true, includes = MoeModule.class)
  class LocalTestModule {
    @Provides public Ui ui() {
      return new RecordingUi();
    }
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
  public void setUp() {
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();
  }

  public void testOneMigration() throws Exception {
    OneMigrationDirective d = new OneMigrationDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().fromRepository = "int(revision=1000)";
    d.getFlags().toRepository = "pub(revision=2)";
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Created Draft Revision: %s", "/dummy/revision/pub"),
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
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
