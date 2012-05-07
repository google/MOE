// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 */
public class OneMigrationDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testOneMigration() throws Exception {
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\":\"foo\",\"repositories\":{" +
        "\"int\":{\"type\":\"dummy\",\"project_space\":\"internal\"}," +
        "\"pub\":{\"type\":\"dummy\"}}," +
        "\"translators\":[{\"from_project_space\":\"internal\"," +
        "\"to_project_space\":\"public\",\"steps\":[{\"name\":\"id_step\"," +
        "\"editor\":{\"type\":\"identity\"}}]}]}");
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
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\":\"foo\",\"repositories\":{" +
        "\"int\":{\"type\":\"dummy\",\"project_space\":\"internal\"}," +
        "\"pub\":{\"type\":\"dummy\"}}," +
        "\"translators\":[{\"from_project_space\":\"internal\"," +
        "\"to_project_space\":\"public\",\"steps\":[{\"name\":\"id_step\"," +
        "\"editor\":{\"type\":\"identity\"}}]}]}");
    OneMigrationDirective d = new OneMigrationDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().fromRepository = "x(revision=1000)";
    d.getFlags().toRepository = "pub(revision=2)";
    try {
      d.perform();
      fail("OneMigrationDirective didn't fail on invalid repository 'x'.");
    } catch (MoeProblem expected) {
      assertEquals("No repository x", expected.getMessage());
    }
  }

  public void testOneMigrationFailOnToRevision() throws Exception {
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\":\"foo\",\"repositories\":{" +
        "\"int\":{\"type\":\"dummy\",\"project_space\":\"internal\"}," +
        "\"pub\":{\"type\":\"dummy\"}}," +
        "\"translators\":[{\"from_project_space\":\"internal\"," +
        "\"to_project_space\":\"public\",\"steps\":[{\"name\":\"id_step\"," +
        "\"editor\":{\"type\":\"identity\"}}]}]}");
    OneMigrationDirective d = new OneMigrationDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().fromRepository = "int(revision=1000)";
    d.getFlags().toRepository = "x(revision=2)";
    assertEquals(1, d.perform());
    assertEquals(
        String.format("No repository x"),
        ((RecordingUi) AppContext.RUN.ui).lastError);
  }
}
