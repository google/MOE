// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ChangeDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testChange() throws Exception {
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    ChangeDirective d = new ChangeDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().codebase = "internal";
    d.getFlags().destination = "internal";
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Created Draft Revision: %s", "/dummy/revision"),
        ((RecordingUi)AppContext.RUN.ui).lastInfo);
  }
}
