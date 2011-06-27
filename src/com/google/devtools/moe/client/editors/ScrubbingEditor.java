// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * A ScrubbingEditor invokes the MOE scrubber on a Codebase.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ScrubbingEditor implements Editor {

  private String name;
  private JsonObject scrubberConfig;
  private File scrubberBinary;

  ScrubbingEditor(String editorName, JsonObject scrubberConfig) {
    name = editorName;
    this.scrubberConfig = scrubberConfig;
    scrubberBinary = null;
  }

  /**
   * Returns a description of what this editor will do.
   */
  public String getDescription() {
    return name;
  }

  File getScrubberBinary() throws IOException {
    if (scrubberBinary != null) {
      return scrubberBinary;
    }
    // TODO(dbentley): what will this resource be under ant?
    scrubberBinary = AppContext.RUN.fileSystem.getResourceAsFile(
        "/devtools/moe/scrubber/scrubber.par");
    AppContext.RUN.fileSystem.setExecutable(scrubberBinary);
    return scrubberBinary;
  }

  /**
   * Edits a Directory.
   *
   * @param input  the directory to edit
   * @param options  command-line parameters
   */
  public File edit(File input, Map<String, String>options) throws CodebaseCreationError {
    File scrubberBinary = null;
    try {
      scrubberBinary = getScrubberBinary();
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }

    File tempDir = AppContext.RUN.fileSystem.getTemporaryDirectory("scrubber_run_");
    File outputTar = new File(tempDir, "scrubbed.tar");

    // For now, we only use the default config
    // TODO(user): allow scrubber command line configuration
    Utils.checkOptionsEmpty(options);
    try {
      AppContext.RUN.cmd.runCommand(
          // The ./ preceding scrubber.par is sometimes needed.
          // TODO(user): figure out why
          "./scrubber.par",
          ImmutableList.of(
              "--temp_dir", tempDir.getAbsolutePath(),
              "--output_tar", outputTar.getAbsolutePath(),
              // TODO(dbentley): allow configuring the scrubber config
              "--config_data", (scrubberConfig == null) ? "{}" : scrubberConfig.toString(),
              input.getAbsolutePath()),
          "", scrubberBinary.getParentFile().getPath());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    File expandedDir = null;
    try {
      expandedDir = Utils.expandTar(outputTar);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    return expandedDir;
  }

  public static ScrubbingEditor makeScrubbingEditor(String editorName, EditorConfig config) {
    return new ScrubbingEditor(editorName, config.getScrubberConfig());
  }

}
