// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 * Unit test for LastEquivalenceDirective.
 *
 */
public class LastEquivalenceDirectiveTest extends TestCase {
  private final InMemoryProjectContextFactory contextFactory = new InMemoryProjectContextFactory();
  private final RecordingUi ui = new RecordingUi();
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = new Injector(null, cmd, contextFactory, ui);
  }

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
    assertEquals(
        "Last equivalence: internal{1} == public{1}",
        ((RecordingUi) Injector.INSTANCE.ui()).lastInfo);
  }
}
