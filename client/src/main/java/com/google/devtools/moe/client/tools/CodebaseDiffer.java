/*
 * Copyright (c) 2015 Google, Inc.
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
package com.google.devtools.moe.client.tools;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;
import java.util.Set;
import javax.inject.Inject;

/** Performs a difference analysis using an underlying {@code FileDiffer}. */
public class CodebaseDiffer {
  private final FileDiffer differ;
  private final FileSystem filesystem;

  @Inject
  public CodebaseDiffer(FileDiffer differ, FileSystem filesystem) {
    this.differ = differ;
    this.filesystem = filesystem;
  }

  /**
   * Diff two {@link Codebase} instances with a {@link FileDiffer}.
   */
  public CodebaseDifference diffCodebases(Codebase codebase1, Codebase codebase2) {
    Set<String> filenames =
        Sets.union(
            Utils.makeFilenamesRelative(filesystem.findFiles(codebase1.root()), codebase1.root()),
            Utils.makeFilenamesRelative(filesystem.findFiles(codebase2.root()), codebase2.root()));

    ImmutableSet.Builder<FileDifference> fileDiffs = ImmutableSet.builder();

    for (String filename : filenames) {
      FileDifference fileDiff =
          differ.diffFiles(filename, codebase1.getFile(filename), codebase2.getFile(filename));
      if (fileDiff.isDifferent()) {
        fileDiffs.add(fileDiff);
      }
    }

    return new CodebaseDifference(codebase1, codebase2, fileDiffs.build());
  }
}
