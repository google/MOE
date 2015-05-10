// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.tools;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;

import java.io.File;

/**
 * Describes the difference between a file in two Codebases.
 *
 * It is a dumb data object, and has no behavior.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class FileDifference {

  public final String relativeFilename;
  public final File file1;
  public final File file2;

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
      if (first == second) return SAME;
      return first ? ONLY1 : ONLY2;
    }
  }

  // There are three ways a pair of files can differ: existence, executability, and content.
  public final Comparison existence;
  public final Comparison executability;
  public final String contentDiff;

  public FileDifference(String relativeFilename, File file1, File file2,
                        Comparison existence, Comparison executability,
                        String contentDiff) {
    this.relativeFilename = relativeFilename;
    this.file1 = file1;
    this.file2 = file2;
    this.existence = existence;
    this.executability = executability;
    this.contentDiff = contentDiff;
  }

  /** @return whether this FileDifference in fact indicates a difference between files */
  public boolean isDifferent() {
    return executability != Comparison.SAME || existence != Comparison.SAME || contentDiff != null;
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

  public static class ConcreteFileDiffer implements FileDiffer {

    @Override
    public FileDifference diffFiles(String relativeFilename, File file1, File file2) {
      // Diff their existence.
      FileSystem fileSystem = Injector.INSTANCE.fileSystem();
      boolean file1Exists = fileSystem.exists(file1);
      boolean file2Exists = fileSystem.exists(file2);

      Preconditions.checkArgument(file1Exists || file2Exists,
                                  "Neither file exists: %s, %s", file1, file2);

      Comparison existence = Comparison.diffBools(file1Exists, file2Exists);

      boolean file1Executable = fileSystem.isExecutable(file1);
      boolean file2Executable = fileSystem.isExecutable(file2);

      Comparison executability = Comparison.diffBools(file1Executable, file2Executable);

      String contentDiff = null;

      try {
        Injector.INSTANCE.cmd().runCommand(
            "diff",
            // -N treats absent files as empty.
            ImmutableList.of("-N", file1.getAbsolutePath(), file2.getAbsolutePath()), "");
      } catch (CommandRunner.CommandException e) {
        if (e.returnStatus != DIFF_ERROR_CODE_FILES_DIFFERENT &&
            e.returnStatus != DIFF_ERROR_CODE_FILES_BINARY) {
          throw new MoeProblem(String.format("diff returned unknown status: %d", e.returnStatus));
        }
        contentDiff = e.stdout;
      }

      return new FileDifference(
          relativeFilename, file1, file2, existence, executability, contentDiff);
    }
  }

  public static FileDiffer CONCRETE_FILE_DIFFER = new ConcreteFileDiffer();
}
