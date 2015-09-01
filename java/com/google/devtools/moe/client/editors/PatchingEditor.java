// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
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

  private final CommandRunner cmd = Injector.INSTANCE.cmd(); // TODO(cgruber) @Inject
  private final FileSystem filesystem = Injector.INSTANCE.fileSystem(); // TODO(cgruber) @Inject
  private final String name;

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
    File tempDir = filesystem.getTemporaryDirectory("patcher_run_");
    String patchFilePath = options.get("file");
    if (isNullOrEmpty(patchFilePath)) {
      return input;
    } else {
      File patchFile = new File(patchFilePath);
      if (!filesystem.isReadable(patchFile)) {
        throw new MoeProblem("cannot read file %s", patchFilePath);
      }
      try {
        Utils.copyDirectory(input.getPath(), tempDir);
      } catch (IOException | CommandRunner.CommandException e) {
        throw new MoeProblem(e.getMessage());
      }
      try {
        cmd.runCommand(
            "patch",
            ImmutableList.of("-p0", "--input=" + patchFilePath),
            tempDir.getAbsolutePath());
      } catch (CommandRunner.CommandException e) {
        throw new MoeProblem(e.getMessage());
      }
      return new Codebase(filesystem, tempDir, input.getProjectSpace(), input.getExpression());
    }
  }

  public static PatchingEditor makePatchingEditor(String editorName, EditorConfig config) {
    // TODO(user): Don't ignore the config
    return new PatchingEditor(editorName);
  }
}
