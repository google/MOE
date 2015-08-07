// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.SystemCommandRunner;
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
  private final InMemoryProjectContextFactory contextFactory = new InMemoryProjectContextFactory();
  private final RecordingUi ui = new RecordingUi();
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = new Injector(null, cmd, contextFactory, ui);
  }

  public void testChange() throws Exception {
    InMemoryProjectContextFactory contextFactory = new InMemoryProjectContextFactory();
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
