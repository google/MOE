// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CheckConfigDirectiveTest extends TestCase {

  public void testEmptyConfigFilenameReturnsOne() throws Exception {
    AppContextForTesting.initForTest();
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "");
    CheckConfigDirective d = new CheckConfigDirective();
    assertEquals(1, d.perform());
  }

  public void testEmptyConfigFileReturnsOne() throws Exception {
    AppContextForTesting.initForTest();
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "");
    CheckConfigDirective d = new CheckConfigDirective();
    d.getFlags().configFilename = "moe_config.txt";
    assertEquals(1, d.perform());
  }

  public void testSimpleConfigFileWorks() throws Exception {
    AppContextForTesting.initForTest();
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": " +
        "{\"public\": {\"type\": \"dummy\"}}}");
    CheckConfigDirective d = new CheckConfigDirective();
    d.getFlags().configFilename = "moe_config.txt";
    assertEquals(0, d.perform());
  }

}
