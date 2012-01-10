// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 * Unit test for LastEquivalenceDirective.
 *
 */
public class LastEquivalenceDirectiveTest extends TestCase {
  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testPerform() throws Exception {
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    LastEquivalenceDirective d = new LastEquivalenceDirective();
    LastEquivalenceDirective.LastEquivalenceOptions options =
        ((LastEquivalenceDirective.LastEquivalenceOptions) d.getFlags());
    options.configFilename = "moe_config.txt";
    options.dbLocation = "dummy";
    options.fromRepository = "internal(revision=1)";
    options.withRepository = "public";
    assertEquals(0, d.perform());
    assertEquals("Last equivalence: internal{1} == public{1}",
                 ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }
}
