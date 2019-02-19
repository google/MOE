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

package com.google.devtools.moe.client.translation.editors;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.config.EditorConfig;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * An editor that will run the shell command specified in the commandString field.
 *
 * <p>Note: this command string can and probably will be a concatenation e.g. "command1 && command2
 * && command3..."
 */
@AutoFactory(implementing = Editor.Factory.class)
public class ShellEditor implements Editor {

  private final CommandRunner cmd;
  private final FileSystem filesystem;
  private final String name;
  private final String commandString;

  ShellEditor(
      @Provided CommandRunner cmd,
      @Provided FileSystem filesystem,
      String name,
      EditorConfig config) {
    this.cmd = cmd;
    this.filesystem = filesystem;
    this.name = name;
    this.commandString = config.commandString();
  }

  /**
   * Returns a description of what this editor will do.
   */
  @Override
  public String getDescription() {
    return "shell step " + name;
  }

  /**
   * Runs this editor's shell commands on a copy of the input Codebase's contents and returns a new
   * Codebase containing the edited contents.
   *
   * @param input the Codebase to edit
   * @param options a map containing any command line options such as a specific revision
   */
  @Override
  public Codebase edit(Codebase input, Map<String, String> options) {
    File tempDir = filesystem.getTemporaryDirectory("shell_run_");
    try {
      filesystem.copyDirectory(input.root(), tempDir);
    } catch (IOException e) {
      throw new MoeProblem(e, "Failed to copy directory %s to %s", input.root(), tempDir);
    }
    try {
      cmd.runCommand(tempDir.getAbsolutePath(), "bash", ImmutableList.of("-c", this.commandString));
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem("Command failed: %s", e.getMessage());
    }
    return Codebase.create(tempDir, input.projectSpace(), input.expression());
  }
}
