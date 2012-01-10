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
  @Override
  public String getDescription() {
    return name;
  }

  /**
   * Applies a patch to copied contents of the input Codebase, returning a new Codebase with the
   * results of the patch.
   */
  @Override
  public Codebase edit(Codebase input, ProjectContext context, Map<String, String> options) {
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
       Utils.copyDirectory(input.getPath(), tempDir);
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
            tempDir.getAbsolutePath());
      } catch (CommandRunner.CommandException e) {
        throw new MoeProblem(e.getMessage());
      }
      return new Codebase(tempDir, input.getProjectSpace(), input.getExpression());
    }
  }

  public static PatchingEditor makePatchingEditor(String editorName, EditorConfig config) {
    // TODO(user): Don't ignore the config
    return new PatchingEditor(editorName);
  }
}
