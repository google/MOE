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
package com.google.devtools.moe.client.tools;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Utilities to allow code to manipulate {@code .tar} files. */
@Singleton
public final class TarUtils {
  private final FileSystem filesystem;
  private final CommandRunner cmd;

  @Inject
  @VisibleForTesting
  public TarUtils(FileSystem filesystem, CommandRunner cmd) {
    this.filesystem = filesystem;
    this.cmd = cmd;
  }

  /**
   * Expands a the {@code .tar} contents of a {@link File} into a temporary working directory, and
   * returns a {@link File} object pointing to that working directory.
   */
  public File expandTar(File tar) throws IOException, CommandException {
    File expandedDir = filesystem.getTemporaryDirectory("expanded_tar_");
    filesystem.makeDirs(expandedDir);
    try {
      cmd.runCommand(
          expandedDir.getAbsolutePath(), "tar", ImmutableList.of("-xf", tar.getAbsolutePath()));
    } catch (CommandRunner.CommandException e) {
      filesystem.deleteRecursively(expandedDir);
      throw e;
    }
    return expandedDir;
  }
}
