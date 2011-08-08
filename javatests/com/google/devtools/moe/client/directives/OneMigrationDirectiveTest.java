// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
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
    d.getFlags().dbLocation = "dummy";
    d.getFlags().fromRevision = "int{1000}";
    d.getFlags().toRevision = "pub{2}";
    d.getFlags().revisionsToMigrate = "int{1000}";
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Created Draft Revision: %s", "/dummy/revision"),
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
    d.getFlags().dbLocation = "dummy";
    d.getFlags().fromRevision = "x{1000}";
    d.getFlags().toRevision = "pub{2}";
    d.getFlags().revisionsToMigrate = "int{1000}";
    assertEquals(1, d.perform());
    assertEquals(
        String.format("Revision Expression Error: No repository x"),
        ((RecordingUi) AppContext.RUN.ui).lastError);
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
    d.getFlags().dbLocation = "dummy";
    d.getFlags().fromRevision = "int{1000}";
    d.getFlags().toRevision = "x{2}";
    d.getFlags().revisionsToMigrate = "int{1000}";
    assertEquals(1, d.perform());
    assertEquals(
        String.format("No repository x"),
        ((RecordingUi) AppContext.RUN.ui).lastError);
  }

  public void testOneMigrationFailOnRevisionsToMigrate() throws Exception {
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
    d.getFlags().dbLocation = "dummy";
    d.getFlags().fromRevision = "int{1000}";
    d.getFlags().toRevision = "pub{2}";
    d.getFlags().revisionsToMigrate = "x{1000}";
    assertEquals(1, d.perform());
    assertEquals(
        String.format("Revision Expression Error: No repository x"),
        ((RecordingUi) AppContext.RUN.ui).lastError);
  }
}
