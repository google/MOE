// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.ExtendedTestModule;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import dagger.ObjectGraph;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class HighestRevisionDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    ObjectGraph graph = ObjectGraph.create(new ExtendedTestModule(null, null));
    graph.injectStatics();
  }

  public void testWithoutRevision() throws Exception {
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    HighestRevisionDirective d = new HighestRevisionDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repository = "internal";
    assertEquals(0, d.perform());
    assertEquals(
        "Highest revision in repository \"internal\": 1",
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }

  public void testWithRevision() throws Exception {
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    HighestRevisionDirective d = new HighestRevisionDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().repository = "internal(revision=4)";
    assertEquals(0, d.perform());
    assertEquals(
        "Highest revision in repository \"internal\": 4",
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }
}
