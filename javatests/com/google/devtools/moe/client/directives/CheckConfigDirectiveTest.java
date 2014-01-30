// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.ExtendedTestModule;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;

import dagger.ObjectGraph;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CheckConfigDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    ObjectGraph graph = ObjectGraph.create(new ExtendedTestModule(null, null));
    graph.injectStatics();
  }

  public void testEmptyConfigFilenameReturnsOne() throws Exception {
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "");
    CheckConfigDirective d = new CheckConfigDirective();
    assertEquals(1, d.perform());
  }

  public void testEmptyConfigFileReturnsOne() throws Exception {
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "");
    CheckConfigDirective d = new CheckConfigDirective();
    d.getFlags().configFilename = "moe_config.txt";
    assertEquals(1, d.perform());
  }

  public void testSimpleConfigFileWorks() throws Exception {
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": " +
        "{\"public\": {\"type\": \"dummy\"}}}");
    CheckConfigDirective d = new CheckConfigDirective();
    d.getFlags().configFilename = "moe_config.txt";
    assertEquals(0, d.perform());
  }

}
