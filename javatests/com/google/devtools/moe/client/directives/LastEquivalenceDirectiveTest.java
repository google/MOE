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
 * Unit test for LastEquivalenceDirective.
 *
 */
public class LastEquivalenceDirectiveTest extends TestCase {
  private final RecordingUi ui = new RecordingUi();
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory(null)));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(cmd, null, ui, repositories);

  public void testPerform() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    LastEquivalenceDirective d = new LastEquivalenceDirective(contextFactory, ui);
    d.setContextFileName("moe_config.txt");
    d.dbLocation = "dummy";
    d.fromRepository = "internal(revision=1)";
    d.withRepository = "public";
    assertEquals(0, d.perform());
    assertEquals("Last equivalence: internal{1} == public{1}", ui.lastInfo);
  }
}