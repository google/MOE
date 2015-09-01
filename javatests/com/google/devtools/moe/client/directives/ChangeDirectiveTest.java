// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;
import com.google.devtools.moe.client.writer.DraftRevision;

import junit.framework.TestCase;

/**
 * Tests for {@link ChangeDirective}.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ChangeDirectiveTest extends TestCase {
  private final RecordingUi ui = new RecordingUi();
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory(null)));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(null, cmd, null, ui, repositories);

  public void testChange() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    ChangeDirective d = new ChangeDirective(contextFactory, ui, new DraftRevision.Factory(ui));
    d.setContextFileName("moe_config.txt");
    d.codebase = "internal";
    d.destination = "internal";
    assertEquals(0, d.perform());
    assertEquals("/dummy/writer/internal", ui.lastTaskResult);
  }
}