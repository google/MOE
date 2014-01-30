// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.ExtendedTestModule;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import dagger.ObjectGraph;

import junit.framework.TestCase;

/**
 */
public class FindEquivalenceDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    ObjectGraph graph = ObjectGraph.create(new ExtendedTestModule(null, null));
    graph.injectStatics();
  }

  public void testFindEquivalenceDirective() throws Exception {
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"test\",\"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    FindEquivalenceDirective d = new FindEquivalenceDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = "dummy";
    d.getFlags().fromRepository = "internal(revision=1)";
    d.getFlags().inRepository = "public";
    assertEquals(0, d.perform());
    assertEquals(
        "\"internal{1}\" == \"public{1,2}\"",
        ((RecordingUi) AppContext.RUN.ui).lastInfo);
  }
}
