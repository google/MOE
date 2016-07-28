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

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;

import java.io.File;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Describes the difference between a file in two Codebases.
 */
@AutoValue
public abstract class FileDifference {
  public abstract String relativeFilename();

  public abstract File file1();

  public abstract File file2();

  // There are three ways a pair of files can differ: existence, executability, and content.
  public abstract Comparison existence();

  public abstract Comparison executability();

  @Nullable
  public abstract String contentDiff();

  public static FileDifference create(
      String relativeFilename,
      File file1,
      File file2,
      Comparison existence,
      Comparison executability,
      @Nullable String contentDiff) {
    return new AutoValue_FileDifference(
        relativeFilename, file1, file2, existence, executability, contentDiff);
  }

  /** @return whether this FileDifference in fact indicates a difference between files */
  public boolean isDifferent() {
    return executability() != Comparison.SAME
        || existence() != Comparison.SAME
        || contentDiff() != null;
  }

  /**
   * A FileDiffer diffs files. This exists as an interface instead of as a static function
   * so that we can mock it out.
   */
  public static interface FileDiffer {
    /**
     * Diffs a file in two codebases.
     *
     * @param relativeFilename  the name of the file in the codebase
     * @param file1  the file in codebase1
     * @param file2  the file in codebase2
     *
     * @result the FileDifference if the files differ, or null if they don't
     */
    public FileDifference diffFiles(String relativeFilename, File file1, File file2);
  }

  /**
   * Error code returned by diff when files are different.
   */
  private static final int DIFF_ERROR_CODE_FILES_DIFFERENT = 1;

  /**
   * Error code returned by diff when at least one file is binary.
   * Note: This error code is also returned when neither file exists, so a separate check must be
   * done to distinguish between the two error cases, if necessary.
   */
  private static final int DIFF_ERROR_CODE_FILES_BINARY = 2;

  /**
   * Describes a comparison between two values.
   */
  public enum Comparison {
    // The values are the same.
    SAME,
    // The value is true only for the first.
    ONLY1,
    // The value is true only for the second.
    ONLY2;

    public static Comparison diffBools(boolean first, boolean second) {
      return (first == second) ? SAME : (first ? ONLY1 : ONLY2);
    }
  }

  /**
   * A {@link FileDiffer} implementation backed by a {@link FileSystem} using the {@code diff}
   * command from a forked command-line.
   */
  public static class ConcreteFileDiffer implements FileDiffer {
    private final CommandRunner cmd;
    private final FileSystem filesystem;

    @Inject
    public ConcreteFileDiffer(CommandRunner cmd, @Nullable FileSystem filesystem) {
      this.cmd = cmd;
      this.filesystem = filesystem;
    }

    @Override
    public FileDifference diffFiles(String relativeFilename, File file1, File file2) {
      // TODO(cgruber): Deal with this nullability leak.
      if (filesystem == null) {
        return null;
      }

      // Diff their existence.
      boolean file1Exists = filesystem.exists(file1);
      boolean file2Exists = filesystem.exists(file2);

      Preconditions.checkArgument(
          file1Exists || file2Exists, "Neither file exists: %s, %s", file1, file2);

      Comparison existence = Comparison.diffBools(file1Exists, file2Exists);

      boolean file1Executable = filesystem.isExecutable(file1);
      boolean file2Executable = filesystem.isExecutable(file2);

      Comparison executability = Comparison.diffBools(file1Executable, file2Executable);

      String contentDiff = null;

      try {
        cmd.runCommand(
            "diff",
            // -N treats absent files as empty.
            ImmutableList.of("-N", "-u", file1.getAbsolutePath(), file2.getAbsolutePath()),
            "");
      } catch (CommandRunner.CommandException e) {
        if (e.returnStatus != DIFF_ERROR_CODE_FILES_DIFFERENT
            && e.returnStatus != DIFF_ERROR_CODE_FILES_BINARY) {
          throw new MoeProblem("diff returned unknown status: %d", e.returnStatus);
        }
        contentDiff = e.stdout;
      }

      return FileDifference.create(
          relativeFilename, file1, file2, existence, executability, contentDiff);
    }
  }
}
