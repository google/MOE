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
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CreateCodebaseDirectiveTest extends TestCase {
  public final RecordingUi ui = new RecordingUi();
  public final SystemCommandRunner cmd = new SystemCommandRunner(ui);
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory()));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(cmd, null, ui, repositories);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = new Injector(null, cmd, contextFactory, ui);
  }

  public void testCreateCodebase() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    CreateCodebaseDirective d = new CreateCodebaseDirective(cmd, contextFactory, ui);
    d.setContextFileName("moe_config.txt");
    d.codebase = "internal";
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Codebase \"%s\" created at %s", "internal", "/dummy/codebase/internal/1"),
        ui.lastInfo);
  }

  public void testCreateCodebaseWithEditors() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {"
            + "\"internal\": {\"type\": \"dummy\"}}, \"editors\": {"
            + "\"identity\": {\"type\":\"identity\"}}}");
    CreateCodebaseDirective d = new CreateCodebaseDirective(cmd, contextFactory, ui);
    d.setContextFileName("moe_config.txt");
    d.codebase = "internal|identity";
    assertEquals(0, d.perform());
    assertEquals(
        String.format(
            "Codebase \"%s\" created at %s", "internal|identity", "/dummy/codebase/internal/1"),
        ui.lastInfo);
  }
}