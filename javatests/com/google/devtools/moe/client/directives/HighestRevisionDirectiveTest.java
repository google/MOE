// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class HighestRevisionDirectiveTest extends TestCase {
  private final RecordingUi ui = new RecordingUi();
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory(null)));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(cmd, null, ui, repositories);

  public void testWithoutRevision() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    HighestRevisionDirective d = new HighestRevisionDirective(contextFactory, ui);
    d.setContextFileName("moe_config.txt");
    d.repository = "internal";
    assertEquals(0, d.perform());
    assertEquals("Highest revision in repository \"internal\": 1", ui.lastInfo);
  }

  public void testWithRevision() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    HighestRevisionDirective d = new HighestRevisionDirective(contextFactory, ui);
    d.setContextFileName("moe_config.txt");
    d.repository = "internal(revision=4)";
    assertEquals(0, d.perform());
    assertEquals("Highest revision in repository \"internal\": 4", ui.lastInfo);
  }
}