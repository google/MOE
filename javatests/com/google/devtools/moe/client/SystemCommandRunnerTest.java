// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner.CommandException;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SystemCommandRunnerTest extends TestCase {

  SystemCommandRunner c;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    c =
        new SystemCommandRunner(
            new SystemUi() {
              @Override
              public void debug(String msg) {}
            });
  }

  public void testLongStdout() throws Exception {
    String data = c.runCommand("perl", ImmutableList.of("-e", "print (\"*\" x 17000)"), "");
    assertEquals(17000, data.length());
  }

  public void testLongStderr() throws Exception {
    String data = c.runCommand("perl", ImmutableList.of("-e", "print STDERR (\"*\" x 17000)"), "");
    assertEquals(0, data.length());
  }

  /**
   * Tests that a process with a large stdout and stderr doesn't produce stream contention or
   * deadlock behavior.
   */
  public void testLongStdoutAndStderr() throws Exception {
    int bytesOutput = 1000000;
    // Sub in the desired output size and exit code for this script with String.format(size, exit).
    String perlScript = "print STDOUT ('*' x %1$d); print STDERR ('*' x %1$d); exit %2$d";

    String stdout =
        c.runCommand("perl", ImmutableList.of("-e", String.format(perlScript, bytesOutput, 0)), "");
    assertEquals(bytesOutput, stdout.length());

    try {
      c.runCommand("perl", ImmutableList.of("-e", String.format(perlScript, bytesOutput, 1)), "");
      fail("Non-zero return code didn't raise CommandException.");

    } catch (CommandException expected) {
      assertEquals("returnStatus", 1, expected.returnStatus);
      assertEquals("stdout length", bytesOutput, expected.stdout.length());
      assertEquals("stderr length", bytesOutput, expected.stderr.length());
    }
  }
}
