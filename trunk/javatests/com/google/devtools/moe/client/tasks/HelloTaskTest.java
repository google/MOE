// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.tasks;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class HelloTaskTest extends TestCase {

  public void testReturnsHello() throws Exception {
    Task<String> t = new HelloTask("Hello World");
    String result = t.execute();
    Task.Explanation e = t.explain(result);
    assertEquals("Hello World", e.message);
    assertEquals(0, e.exitCode);
  }
}
