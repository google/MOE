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

package com.google.devtools.moe.client.editors;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.gson.GsonModule;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ScrubberConfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * A ScrubbingEditor invokes the MOE scrubber on a Codebase.
 */
public class ScrubbingEditor implements Editor {

  private final CommandRunner cmd = Injector.INSTANCE.cmd(); // TODO(cgruber) @Inject
  private final FileSystem filesystem = Injector.INSTANCE.fileSystem(); // TODO(cgruber) @Inject

  /**
   * A {@code Supplier} that extracts the scrubber binary. We use a Supplier because we don't want
   * to extract the scrubber until it's needed. (A run of MOE may initialize a project context and
   * instantiate editors without actually editing.) It is memoized because we only need one copy of
   * the scrubber binary across MOE execution.
   */
  // TODO(cgruber): Inject this
  private static final Supplier<File> SCRUBBER_BINARY_SUPPLIER =
      Suppliers.memoize(
          new Supplier<File>() {
            @Override
            public File get() {
              try {
                // TODO(dbentley): what will this resource be under ant?
                File scrubberBinary =
                    Injector.INSTANCE
                        .fileSystem()
                        .getResourceAsFile("/devtools/moe/scrubber/scrubber.par");
                Injector.INSTANCE.fileSystem().setExecutable(scrubberBinary);
                return scrubberBinary;
              } catch (IOException ioEx) {
                throw new MoeProblem(ioEx, "Error extracting scrubber.");
              }
            }
          });

  private final String name;
  private final ScrubberConfig scrubberConfig;

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
    File tempDir = filesystem.getTemporaryDirectory("scrubber_run_");
    File outputTar = new File(tempDir, "scrubbed.tar");
    try {
      cmd.runCommand(
          // The ./ preceding scrubber.par is sometimes needed.
          // TODO(user): figure out why
          "./scrubber.par",
          ImmutableList.of(
              "--temp_dir",
              tempDir.getAbsolutePath(),
              "--output_tar",
              outputTar.getAbsolutePath(),
              // TODO(dbentley): allow configuring the scrubber config
              "--config_data",
              (scrubberConfig == null) ? "{}" : GsonModule.provideGson().toJson(scrubberConfig),
              // TODO(cgruber): Eliminate this static gson method reference.
              input.getPath().getAbsolutePath()),
          SCRUBBER_BINARY_SUPPLIER.get().getParentFile().getPath());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    File expandedDir = null;
    try {
      expandedDir = Utils.expandTar(outputTar);
    } catch (IOException | CommandRunner.CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    return new Codebase(filesystem, expandedDir, input.getProjectSpace(), input.getExpression());
  }

  public static ScrubbingEditor makeScrubbingEditor(String editorName, EditorConfig config) {
    return new ScrubbingEditor(editorName, config.scrubberConfig());
  }
}
