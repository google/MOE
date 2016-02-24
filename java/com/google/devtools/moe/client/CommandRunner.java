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

import java.util.List;

/**
 * A wrapper around call-outs to out-of-process executable programs.
 */
public interface CommandRunner {

  /**
   * Runs a command.
   *
   * @param command the binary to invoke. If not a path, it will be resolved.
   * @param args the arguments to pass to the binary.
   * @param workingDirectory the directory to run in.
   * 
   * @return the output of the command.
   * @throws CommandException if some error occurs in the command execution.
   */
  // TODO(dbentley): make it easier to do error-handling
  public String runCommand(String command, List<String> args, String workingDirectory) 
      throws CommandException;

  /**
   * Runs a command.
   *
   * @param command the binary to invoke. If not a path, it will be resolved.
   * @param args the arguments to pass to the binary.
   * @param workingDirectory the directory to run in.
   *
   * @return a {@link CommandOutput} with the full results of the command.
   * @throws CommandException if some error occurs in the command execution.
   */
  public CommandOutput runCommandWithFullOutput(String command, List<String> args, 
      String workingDirectory) throws CommandException;
}
