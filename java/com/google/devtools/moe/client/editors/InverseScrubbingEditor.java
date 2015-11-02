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

import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseMerger;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;

import java.util.Map;

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
public class InverseScrubbingEditor implements InverseEditor {
  private final FileDiffer differ;
  private final CommandRunner cmd;
  private final FileSystem filesystem;
  private final Ui ui;

  public InverseScrubbingEditor(
      FileDiffer differ, CommandRunner cmd, FileSystem filesystem, Ui ui) {
    this.differ = differ;
    this.cmd = cmd;
    this.filesystem = filesystem;
    this.ui = ui;
  }

  @Override
  public Codebase inverseEdit(
      Codebase input,
      Codebase referenceFrom,
      Codebase referenceTo,
      ProjectContext context,
      Map<String, String> options) {
    CodebaseMerger merger =
        new CodebaseMerger(ui, filesystem, cmd, differ, referenceFrom, input, referenceTo);
    return merger.merge();
  }
}