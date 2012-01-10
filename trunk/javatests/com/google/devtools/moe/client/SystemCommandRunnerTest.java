// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SystemCommandRunnerTest extends TestCase {

  SystemCommandRunner c;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    c = new SystemCommandRunner();
  }

  public void testLongStdout() throws Exception {
    String data = c.runCommand(
        "perl", ImmutableList.of("-e", "print (\"*\" x 17000)"), "");
    assertEquals(17000, data.length());
  }

  public void testLongStderr() throws Exception {
    String data = c.runCommand(
        "perl", ImmutableList.of("-e", "print STDERR (\"*\" x 17000)"), "");
    assertEquals(0, data.length());
  }
}
