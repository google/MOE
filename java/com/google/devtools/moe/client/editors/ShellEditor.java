// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.project.EditorConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
   * Edits a Directory. Copies the input, runs the shell commands and
   * returns the result.
   *
   * @param input  the directory to edit
   * @param options  command-line parameters
   */
  @Override
  public File edit(File input, Map<String, String> options) {
    File tempDir = AppContext.RUN.fileSystem.getTemporaryDirectory("shell_run_");
    try {
     Utils.copyDirectory(input, tempDir);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    try {
      List<String> argsList = new Vector<String>();
      argsList.add("-c");
      argsList.add(this.commandString);
      AppContext.RUN.cmd.runCommand("bash", argsList, "", tempDir.getAbsolutePath());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    return tempDir;
  }

  public static ShellEditor makeShellEditor(String editorName, EditorConfig config) {
    return new ShellEditor(editorName, config.getCommandString());
  }

}
