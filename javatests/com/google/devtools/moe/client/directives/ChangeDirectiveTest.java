// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.ExtendedTestModule;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import dagger.ObjectGraph;

import junit.framework.TestCase;

/**
 * Tests for {@link ChangeDirective}.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ChangeDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    ObjectGraph graph = ObjectGraph.create(new ExtendedTestModule(null, null));
    graph.injectStatics();
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
    assertEquals("/dummy/writer/internal", ((RecordingUi) AppContext.RUN.ui).lastTaskResult);
  }
}
