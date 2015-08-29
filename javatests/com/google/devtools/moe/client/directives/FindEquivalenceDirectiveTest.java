// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 */
public class FindEquivalenceDirectiveTest extends TestCase {
  private final RecordingUi ui = new RecordingUi();
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory()));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(cmd, null, ui, repositories);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = new Injector(null, cmd, contextFactory, ui);
  }

  public void testFindEquivalenceDirective() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"test\",\"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    FindEquivalenceDirective d = new FindEquivalenceDirective(contextFactory, ui);
    d.setContextFileName("moe_config.txt");
    d.dbLocation = "dummy";
    d.fromRepository = "internal(revision=1)";
    d.inRepository = "public";
    assertEquals(0, d.perform());
    assertEquals(
        "\"internal{1}\" == \"public{1,2}\"", ((RecordingUi) Injector.INSTANCE.ui()).lastInfo);
  }
}