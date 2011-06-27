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
public class HighestRevisionDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testWithoutRevision() throws Exception {
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    HighestRevisionDirective d = new HighestRevisionDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repository = "internal";
    assertEquals(0, d.perform());
    assertEquals(
        "Highest revision in repository \"internal\": 1",
        ((RecordingUi)AppContext.RUN.ui).lastInfo);
  }

  public void testWithRevision() throws Exception {
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    HighestRevisionDirective d = new HighestRevisionDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repository = "internal";
    d.getFlags().revision = "4";
    assertEquals(0, d.perform());
    assertEquals(
        "Highest revision in repository \"internal\": 4",
        ((RecordingUi)AppContext.RUN.ui).lastInfo);
  }

}
