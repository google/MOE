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

import java.util.Collections;
import java.util.List;

/**
 * A wrapper around call-outs to out-of-process executable programs.
 */
public interface CommandRunner {

  public static class CommandException extends Exception {
    public final String cmd;
    public final List<String> args;
    public final String stdout;
    public final String stderr;
    public final int returnStatus;

    public CommandException(
        String cmd, List<String> args, String stdout, String stderr, int returnStatus) {
      super(
          String.format(
              "Running %s with args %s returned %d with stdout %s and stderr %s",
              cmd,
              args,
              returnStatus,
              stdout,
              stderr));
      this.cmd = cmd;
      this.args = Collections.unmodifiableList(args);
      this.stdout = stdout;
      this.stderr = stderr;
      this.returnStatus = returnStatus;
    }
  }

  /**
   * The complete result, including stdout and stderr, of running a command.
   */
  public static class CommandOutput {
    private final String stdout;
    private final String stderr;

    public CommandOutput(String stdout, String stderr) {
      this.stdout = stdout;
      this.stderr = stderr;
    }

    public String getStdout() {
      return stdout;
    }

    public String getStderr() {
      return stderr;
    }
  }

  /**
   * Runs a command.
   *
   * @param workingDirectory the directory to run in
   * @param command the binary to invoke. If not a path, it will be resolved.
   * @param args the arguments to pass to the binary
   * @returns the output of the command
   * @throws CommandException
   */
  // TODO(dbentley): make it easier to do error-handling
  String runCommand(String workingDirectory, String command, List<String> args)
      throws CommandException;

  /**
   * Runs a command.
   *
   * @param workingDirectory the directory to run in
   * @param command the binary to invoke. If not a path, it will be resolved.
   * @param args the arguments to pass to the binary
   * @returns a {@link CommandOutput} with the full results of the command
   * @throws CommandException
   */
  CommandOutput runCommandWithFullOutput(String workingDirectory, String command, List<String> args)
      throws CommandException;
}
