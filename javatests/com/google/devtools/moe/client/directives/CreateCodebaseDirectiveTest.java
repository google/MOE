// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CreateCodebaseDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testCreateCodebase() throws Exception {
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}}");
    CreateCodebaseDirective d = new CreateCodebaseDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().codebase = "internal";
    assertEquals(0, d.perform());
    assertEquals(
        String.format("Codebase \"%s\" created at %s", "internal", "/dummy/codebase/internal/1"),
        ((RecordingUi)AppContext.RUN.ui).lastInfo);
  }

  public void testCreateCodebaseWithEditors() throws Exception {
    ((InMemoryProjectContextFactory)AppContext.RUN.contextFactory).projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {" +
        "\"internal\": {\"type\": \"dummy\"}}, \"editors\": {" +
        "\"identity\": {\"type\":\"identity\"}}}");
    CreateCodebaseDirective d = new CreateCodebaseDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().codebase = "internal|identity";
    assertEquals(0, d.perform());
    assertEquals(
        String.format(
            "Codebase \"%s\" created at %s", "internal|identity", "/dummy/codebase/internal/1"),
        ((RecordingUi)AppContext.RUN.ui).lastInfo);
  }
}
