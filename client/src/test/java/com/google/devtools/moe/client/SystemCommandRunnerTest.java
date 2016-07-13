/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner.CommandException;

import junit.framework.TestCase;

public class SystemCommandRunnerTest extends TestCase {

  private final SystemCommandRunner c = new SystemCommandRunner();

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
