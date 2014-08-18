// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.devtools.moe.client.project.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ScrubberConfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * A ScrubbingEditor invokes the MOE scrubber on a Codebase.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ScrubbingEditor implements Editor {

  /**
   * A {@code Supplier} that extracts the scrubber binary. We use a Supplier because we don't want
   * to extract the scrubber until it's needed. (A run of MOE may initialize a project context and
   * instantiate editors without actually editing.) It is memoized because we only need one copy of
   * the scrubber binary across MOE execution.
   */
  private static final Supplier<File> SCRUBBER_BINARY_SUPPLIER = Suppliers.memoize(
      new Supplier<File>() {
        @Override public File get() {
          try {
            // TODO(dbentley): what will this resource be under ant?
            File scrubberBinary =
                AppContext.RUN.fileSystem.getResourceAsFile("/devtools/moe/scrubber/scrubber.par");
            AppContext.RUN.fileSystem.setExecutable(scrubberBinary);
            return scrubberBinary;
          } catch (IOException ioEx) {
            AppContext.RUN.ui.error(ioEx, "Error extracting scrubber");
            throw new MoeProblem("Error extracting scrubber: " + ioEx.getMessage());
          }
        }
      });

  private String name;
  private ScrubberConfig scrubberConfig;

  ScrubbingEditor(String editorName, ScrubberConfig scrubberConfig) {
    name = editorName;
    this.scrubberConfig = scrubberConfig;
  }

  /**
   * Returns a description of what this editor will do.
   */
  @Override
  public String getDescription() {
    return name;
  }

  /**
   * Runs the Moe scrubber on the copied contents of the input Codebase and returns a new Codebase
   * with the results of the scrub.
   */
  @Override
  public Codebase edit(Codebase input, ProjectContext context, Map<String, String> options) {
    File tempDir = AppContext.RUN.fileSystem.getTemporaryDirectory("scrubber_run_");
    File outputTar = new File(tempDir, "scrubbed.tar");

    try {
      AppContext.RUN.cmd.runCommand(
          // The ./ preceding scrubber.par is sometimes needed.
          // TODO(user): figure out why
          "./scrubber.par",
          ImmutableList.of(
              "--temp_dir", tempDir.getAbsolutePath(),
              "--output_tar", outputTar.getAbsolutePath(),
              // TODO(dbentley): allow configuring the scrubber config
              "--config_data",
              (scrubberConfig == null) ? "{}" : ProjectConfig.makeGson().toJson(scrubberConfig),
              input.getPath().getAbsolutePath()),
          SCRUBBER_BINARY_SUPPLIER.get().getParentFile().getPath());
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
    return new Codebase(expandedDir, input.getProjectSpace(), input.getExpression());
  }

  public static ScrubbingEditor makeScrubbingEditor(String editorName, EditorConfig config) {
    return new ScrubbingEditor(editorName, config.getScrubberConfig());
  }
}
