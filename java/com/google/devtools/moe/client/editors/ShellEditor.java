// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.devtools.moe.client.project.ProjectContext;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * An editor that will run the shell command specified in the
 * commandString field.
 *
 * Note: this command string can and probably will be a
 * concatenation e.g. "command1 && command2 && command3..."
 *
 */
public class ShellEditor implements Editor {

  private String name, commandString;

  ShellEditor(String editorName, String commandString) {
    name = editorName;
    this.commandString = commandString;
  }

  /**
   * Returns a description of what this editor will do.
   */
  @Override
  public String getDescription() {
    return String.format("shell step %s", name);
  }

  /**
   * Runs this editor's shell commands on a copy of the input Codebase's contents and returns a
   * new Codebase containing the edited contents.
   *
   * @param input the Codebase to edit
   * @param context the ProjectContext for this project
   * @param options a map containing any command line options such as a specific revision
   */
  @Override
  public Codebase edit(Codebase input, ProjectContext context, Map<String, String> options) {
    File tempDir = AppContext.RUN.fileSystem.getTemporaryDirectory("shell_run_");
    try {
     Utils.copyDirectory(input.getPath(), tempDir);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    try {
      AppContext.RUN.cmd.runCommand("bash", ImmutableList.of("-c", this.commandString),
          tempDir.getAbsolutePath());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    return new Codebase(tempDir, input.getProjectSpace(), input.getExpression());
  }

  public static ShellEditor makeShellEditor(String editorName, EditorConfig config) {
    return new ShellEditor(editorName, config.getCommandString());
  }
}
