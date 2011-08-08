// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.project.EditorConfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * A PatchingEditor invokes the command patch
 *
 */
public class PatchingEditor implements Editor {

  private String name;

  PatchingEditor(String editorName) {
    name = editorName;
  }

  /**
   * Returns a description of what this editor will do.
   */
  public String getDescription() {
    return name;
  }

  /**
   * Edits a Directory.
   *
   * @param input  the directory to edit
   * @param options  command-line parameters
   */
  public File edit(File input, Map<String, String> options) throws CodebaseCreationError {
    File tempDir = AppContext.RUN.fileSystem.getTemporaryDirectory("patcher_run_");
    String patchFilePath = options.get("file");
    if (patchFilePath == null || patchFilePath.equals("")) {
      return input;
    } else {
      File patchFile = new File(patchFilePath);
      if (!AppContext.RUN.fileSystem.isReadable(patchFile)) {
        throw new MoeProblem(String.format(
            "cannot read file %s", patchFilePath));
      }
      try {
       Utils.copyDirectory(input, tempDir);
      } catch (IOException e) {
        throw new MoeProblem(e.getMessage());
      } catch (CommandRunner.CommandException e) {
        throw new MoeProblem(e.getMessage());
      }
      try {
        AppContext.RUN.cmd.runCommand(
            "patch",
            ImmutableList.of(
                "-p0",
                "--input=" + patchFilePath),
            "",
            tempDir.getAbsolutePath());
      } catch (CommandRunner.CommandException e) {
        throw new MoeProblem(e.getMessage());
      }
      return tempDir;
    }
  }

  public static PatchingEditor makePatchingEditor(String editorName, EditorConfig config) {
    // TODO(user): Don't ignore the config
    return new PatchingEditor(editorName);
  }

}
