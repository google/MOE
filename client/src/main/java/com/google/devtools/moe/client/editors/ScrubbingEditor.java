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

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Utils.TarUtils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseMerger;
import com.google.devtools.moe.client.gson.GsonModule;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ScrubberConfig;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;
import dagger.Lazy;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.inject.Named;

/** A ScrubbingEditor invokes the MOE scrubber on a Codebase. */
@AutoFactory(implementing = Editor.Factory.class)
public class ScrubbingEditor implements Editor, InverseEditor {
  private final CommandRunner cmd;
  private final FileSystem filesystem;
  private final Ui ui;
  private final Lazy<File> executable;
  private final FileDiffer differ;
  private final String name;
  private final ScrubberConfig scrubberConfig;
  private final TarUtils tarUtils;

  ScrubbingEditor(
      @Provided CommandRunner cmd,
      @Provided FileSystem filesystem,
      @Provided Ui ui,
      @Named("scrubber_binary") @Provided Lazy<File> executable,
      @Provided FileDiffer differ,
      @Provided TarUtils tarUtils,
      String editorName,
      EditorConfig config) {
    this.cmd = cmd;
    this.filesystem = filesystem;
    this.ui = ui;
    this.executable = executable;
    this.differ = differ;
    this.tarUtils = tarUtils;
    this.name = editorName;
    this.scrubberConfig = config.scrubberConfig();
  }

  /**
   * Returns a description of what this editor will do.
   */
  @Override
  public String getDescription() {
    return name;
  }

  @Override
  public InverseEditor validateInversion() throws InvalidProject {
    return this;
  }

  /**
   * Runs the Moe scrubber on the copied contents of the input Codebase and returns a new Codebase
   * with the results of the scrub.
   */
  @Override
  public Codebase edit(Codebase input, Map<String, String> options) {
    File tempDir = filesystem.getTemporaryDirectory("scrubber_run_");
    File outputTar = new File(tempDir, "scrubbed.tar");
    try {
      cmd.runCommand(
          executable.get().getCanonicalPath(),
          ImmutableList.of(
              "--temp_dir",
              tempDir.getAbsolutePath(),
              "--output_tar",
              outputTar.getAbsolutePath(),
              // TODO(dbentley): allow configuring the scrubber config
              "--config_data",
              (scrubberConfig == null) ? "{}" : GsonModule.provideGson().toJson(scrubberConfig),
              // TODO(cgruber): Eliminate this static gson method reference.
              input.path().getAbsolutePath()),
          executable.get().getParentFile().getPath());
    } catch (CommandRunner.CommandException | IOException e) {
      throw new MoeProblem(e, "Problem executing the scrubber: %s", e.getMessage());
    }
    File expandedDir = null;
    try {
      expandedDir = tarUtils.expandTar(outputTar);
    } catch (IOException | CommandRunner.CommandException e) {
      throw new MoeProblem(e.getMessage());
    }
    return Codebase.create(expandedDir, input.projectSpace(), input.expression());
  }

  /**
   * An editor that inverts scrubbing via merging.
   *
   * <p>Say a repository 'internal' is translated to 'public' by scrubbing. Say there is an
   * equivalence internal(x) == public(y), where x and y are revision numbers. We want to port a
   * change public(y+1) by inverse-scrubbing to produce internal(x+1). We do this by merging two
   * sets of changes onto public(y):
   *
   * <ol>
   * <li>internal(x), which change represents the addition of all scrubbed content
   * <li>public(y+1), which is the new public change to apply to the internal codebase
   * </ol>
   *
   * <p>The result of 'merge internal(x) public(y) public(y+1)' is the combined addition of scrubbed
   * content and the new public change. This merge produces internal(x+1).
   */
  @Override
  public Codebase inverseEdit(
      Codebase input, Codebase referenceFrom, Codebase referenceTo, Map<String, String> options) {
    CodebaseMerger merger =
        new CodebaseMerger(ui, filesystem, cmd, differ, referenceFrom, input, referenceTo);
    return merger.merge();
  }
}
