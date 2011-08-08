// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 */
public class FindEquivalenceDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testFindEquivalenceDirective() throws Exception {
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"test\",\"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    FindEquivalenceDirective d = new FindEquivalenceDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = "dummy";
    d.getFlags().revision = "internal{1}";
    d.getFlags().inRepository = "public";
    assertEquals(0, d.perform());
    assertEquals(
        "\"internal{1}\" == \"public{1,2}\"",
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }
}
