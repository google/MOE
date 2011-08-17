// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 */
public class MigrateDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testMigrate() throws Exception {
    // This MOE config contains:
    //  - dummy internal and public repositories (int and pub, respectively)
    //  - a translator from internal to public consisting of a single identity step
    //  - a migration named 'test' from int to pub with no additional config info
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\":\"foo\",\"repositories\":{" +
        "\"int\":{\"type\":\"dummy\",\"project_space\":\"internal\"}," +
        "\"pub\":{\"type\":\"dummy\"}}," +
        "\"translators\":[{\"from_project_space\":\"internal\"," +
        "\"to_project_space\":\"public\",\"steps\":[{\"name\":\"id_step\"," +
        "\"editor\":{\"type\":\"identity\"}}]}]," +
        "\"migrations\":[{\"name\":\"test\",\"from_repository\":\"int\"," +
        "\"to_repository\":\"pub\"}]}");
    MigrateDirective d = new MigrateDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = "dummy";
    d.getFlags().names = ImmutableList.of("test");
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Created Draft Revisions:\n%s in repository %s", "/dummy/revision", "pub"),
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }

  public void testMultipleMigrate() throws Exception {
    // This MOE config contains:
    //  - dummy internal and public repositories (int and pub, respectively)
    //  - a translator from internal to public consisting of a single identity step
    //  - a migration named 'test' from int to pub with no additional config info
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\":\"foo\",\"repositories\":{" +
        "\"int\":{\"type\":\"dummy\",\"project_space\":\"internal\"}," +
        "\"pub\":{\"type\":\"dummy\"}}," +
        "\"translators\":[{\"from_project_space\":\"internal\"," +
        "\"to_project_space\":\"public\",\"steps\":[{\"name\":\"id_step\"," +
        "\"editor\":{\"type\":\"identity\"}}]}]," +
        "\"migrations\":[{\"name\":\"test\",\"from_repository\":\"int\"," +
        "\"to_repository\":\"pub\"}]}");
    MigrateDirective d = new MigrateDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = "dummy";
    d.getFlags().names = ImmutableList.of("test", "test");
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Created Draft Revisions:\n%s in repository %s\n%s in repository %s",
                      "/dummy/revision", "pub", "/dummy/revision", "pub"),
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }

  public void testMigrateAll() throws Exception {
    // This MOE config contains:
    //  - dummy internal and public repositories (int and pub, respectively)
    //  - a translator from internal to public consisting of a single identity step
    //  - a migration named 'test' from int to pub with no additional config info
    //  - a migration named 'test2' from int to pub with no additional config info
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\":\"foo\",\"repositories\":{" +
        "\"int\":{\"type\":\"dummy\",\"project_space\":\"internal\"}," +
        "\"pub\":{\"type\":\"dummy\"}}," +
        "\"translators\":[{\"from_project_space\":\"internal\"," +
        "\"to_project_space\":\"public\",\"steps\":[{\"name\":\"id_step\"," +
        "\"editor\":{\"type\":\"identity\"}}]}]," +
        "\"migrations\":[{\"name\":\"test\",\"from_repository\":\"int\"," +
        "\"to_repository\":\"pub\"},{\"name\":\"test2\",\"from_repository\":\"int\"," +
        "\"to_repository\":\"pub\"}]}");
    MigrateDirective d = new MigrateDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = "dummy";
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Created Draft Revisions:\n%s in repository %s\n%s in repository %s",
                      "/dummy/revision", "pub", "/dummy/revision", "pub"),
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }

  public void testMigrateWithScrubberConfig() throws Exception {
    // This MOE config contains:
    //  - dummy internal and public repositories (int and pub, respectively)
    //  - a translator from internal to public consisting of a single identity step
    //  - a migration named 'test' from int to pub with a scrubber config
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\":\"foo\",\"repositories\":{" +
        "\"int\":{\"type\":\"dummy\",\"project_space\":\"internal\"}," +
        "\"pub\":{\"type\":\"dummy\"}}," +
        "\"translators\":[{\"from_project_space\":\"internal\"," +
        "\"to_project_space\":\"public\",\"steps\":[{\"name\":\"id_step\"," +
        "\"editor\":{\"type\":\"identity\"}}]}]," +
        "\"migrations\":[{\"name\":\"test\",\"from_repository\":\"int\"," +
        "\"to_repository\":\"pub\",\"metadata_scrubber_config\":{" +
        "\"usernames_to_scrub\":[\"user\"],\"scrub_confidential_words\":true}}]}");
    MigrateDirective d = new MigrateDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = "dummy";
    d.getFlags().names = ImmutableList.of("test");
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Created Draft Revisions:\n%s in repository %s", "/dummy/revision", "pub"),
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }
}
