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

package com.google.devtools.moe.client.codebase;

import static com.google.devtools.moe.client.Utils.makeFilenamesRelative;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Merges all changes that lead from {@code originalCodebase} to {@code modifiedCodebase} into
 * destinationCodebase when its {@link #merge(Codebase, Codebase, Codebase)} method is invoked.
 *
 * <p>Here is a description of the UNIX merge(1) tool from its man page:
 *
 * <pre>{@code
 * merge [ options ] file1 file2 file3
 *
 * merge incorporates all changes that lead from file2 to file3 into file1.  The result
 * ordinarily goes into file1.  merge is useful for combining separate changes to an original.
 * Suppose file2 is the original, and both file1 and file3 are modifications of file2.  Then
 * merge combines both changes.
 *
 * }</pre>
 *
 * <p>{@link CodebaseMerger#merge(Codebase, Codebase, Codebase)} performs this type of merge on each
 * file in the three codebases. In {@link CodebaseMerger#merge(Codebase, Codebase, Codebase)},
 * {@code originalCodebase} is analogous to {@code file2}, {@code modifiedCodebase} is analogous to
 * {@code file3}, and {@code destinationCodebase} is analogous to {@code file1}. The output of
 * {@link CodebaseMerger#merge(Codebase, Codebase, Codebase)} is a {@link MergeResult} which
 * contains a {@link Codebase} that incorporates the changes that both {@code modifiedCodebase} and
 * {@code destinationCodebase} made on the {@code originalCodebase}. The differences between {@code
 * modifiedCodebase} and the {@code originalCodebase} are brought into a copy of {@code
 * destinationCodebase}. The result is the merged codebase.
 *
 * <p>This is useful when bringing changes to the public repository into the internal repository.
 * For example, say you run:
 *
 * <pre>{@code
 * merge_codebases --originalCodebase "publicrepo(revision=142)"
 *                 --modifiedCodebase "publicrepo(revision=143)"
 *                 --destinationCodebase "internalrepo(revision=74)"
 * }</pre>
 *
 * <p>Let internalrepo(revision=74) be in equivalence with publicrepo(revision=142). That is, let
 * publicrepo(revision=142) represent the same state of the code as internalrepo(revision=74) minus
 * any confidential code that may have been scrubbed during translation. That means that
 * publicrepo(revision=143) is a change to the public repository which has yet to be brought to the
 * internal repository. By running the above merge_codebases, the changes from the public revision
 * 142 to 143 will be merged into a copy of internal revision 74. The result is an internal revision
 * 75 which has the new public changes and still has the confidential code that a public revision
 * wouldn't have. Thus, internal revision 75 would be equivalent with public revision 143 assuming
 * there were no conflicts when merging.
 */
@Singleton
public class CodebaseMerger {
  private final Ui ui;
  private final FileSystem filesystem;
  private final CommandRunner cmd;
  private final FileDiffer differ;

  @Inject
  CodebaseMerger(Ui ui, FileSystem filesystem, CommandRunner cmd, FileDiffer differ) {
    this.ui = ui;
    this.filesystem = filesystem;
    this.cmd = cmd;
    this.differ = differ;
  }

  /**
   * For each file in the union of the modified and destination codebases, run
   * generateMergedFile(...) and then reports the results when complete.
   *
   * @return the merge result containing the merged {@link Codebase} plus sets of successfully and
   *     unsuccessfully merged files.
   */
  public MergeResult merge(Codebase original, Codebase modified, Codebase destination) {
    MergeResult.Builder resultBuilder = MergeResult.builder();
    File mergedDir = filesystem.getTemporaryDirectory("merged_codebase_");
    RepositoryExpression mergedExpression = new RepositoryExpression("merged");
    resultBuilder.setMergedCodebase(Codebase.create(mergedDir, "merged", mergedExpression));
    Set<String> filesToMerge = Sets.union(findFiles(destination), findFiles(modified));

    for (String filename : filesToMerge) {
      this.generateMergedFile(original, modified, destination, resultBuilder, filename);
    }
    MergeResult result = resultBuilder.build();
    result.report(ui);
    return result;
  }

  private Set<String> findFiles(Codebase codebase) {
    return makeFilenamesRelative(filesystem.findFiles(codebase.root()), codebase.root());
  }

  private boolean areDifferent(String filename, File x, File y) {
    return differ.diffFiles(filename, x, y).isDifferent();
  }

  /**
   * Copy the destFile into the merged codebase. This is where the output of merge will be written
   * to.
   */
  private File copyToMergedCodebase(Codebase merged, String filename, File destFile) {
    File mergedFile = merged.getFile(filename);
    try {
      filesystem.makeDirsForFile(mergedFile);
      filesystem.copyFile(destFile, mergedFile);
      return mergedFile;
    } catch (IOException e) {
      throw new MoeProblem(e, "Failed to copy %s to %s", filename, destFile);
    }
  }

  /**
   * Given a filename, this method finds the file with that name in each of the three codebases.
   * Using the UNIX merge(1) tool, those three files are merged and the result is placed in the
   * merged codebase. Any conflicts that occurred during merging will appear in the merged codebase
   * file for the user to resolve.
   *
   * <p>In the case where the file specified by the given filename exists in the original codebase
   * and in either the modified codebase or the destination codebase (but not both) and if the file
   * is unchanged between those codebases, then a file in the merged codebase will NOT be created
   * and this method will return leaving the merged codebase unchanged.
   *
   * @param original the original codebase in whose context the merge takes place
   * @param modified the modified codebase containing some of the merge content
   * @param destination the destination codebase containing some of the merge content
   * @param resultBuilder the result object into which the merge results (merged file, errors)
   *     should be inserted
   * @param filename the name of the file to merge
   */
  void generateMergedFile(
      Codebase original,
      Codebase modified,
      Codebase destination,
      MergeResult.Builder resultBuilder,
      String filename) {
    File origFile = original.getFile(filename);
    boolean origExists = filesystem.exists(origFile);

    File modFile = modified.getFile(filename);
    boolean modExists = filesystem.exists(modFile);

    File destFile = destination.getFile(filename);
    boolean destExists = filesystem.exists(destFile);

    if (!destExists && !modExists) {
      // This should never be thrown since generateMergedFile(...) is only called on filesToMerge
      // from merge() which is the union of the files in the destination and modified codebases.
      throw new MoeProblem(
          "%s doesn't exist in either %s nor %s. This should not be possible.",
          filename, destination, modified);

    } else if (origExists && modExists && !destExists) {
      if (areDifferent(filename, origFile, modFile)) {
        // Proceed and merge in /dev/null, which should produce a merge conflict (incoming edit on
        // delete).
        destFile = new File("/dev/null");
      } else {
        // Defer to deletion in destination codebase.
        return;
      }

    } else if (origExists && !modExists && destExists) {
      // Blindly follow deletion of the original file by not copying it into the merged codebase.
      return;

    } else if (!origExists && !(modExists && destExists)) {
      // File exists only in modified or destination codebase, so just copy it over.
      File existingFile = (modExists ? modFile : destFile);
      copyToMergedCodebase(resultBuilder.mergedCodebase(), filename, existingFile);
      return;

    } else if (!origExists && modExists && destExists) {
      // Merge both new files (conflict expected).
      origFile = new File("/dev/null");
    }

    File mergedFile = copyToMergedCodebase(resultBuilder.mergedCodebase(), filename, destFile);

    try {
      // Merges the changes that lead from origFile to modFile into mergedFile (which is a copy
      // of destFile). After, mergedFile will have the combined changes of modFile and destFile.
      cmd.runCommand(
          resultBuilder.mergedCodebase().root().getAbsolutePath(),
          "merge",
          ImmutableList.of(
              mergedFile.getAbsolutePath(), origFile.getAbsolutePath(), modFile.getAbsolutePath()));
      // Return status was 0 and the merge was successful. Note it.
      resultBuilder.mergedFilesBuilder().add(mergedFile.getAbsolutePath());
    } catch (CommandException e) {
      // If merge fails with exit status 1, then a conflict occurred. Make a note of the filepath.
      if (e.returnStatus == 1) {
        resultBuilder.failedFilesBuilder().add(mergedFile.getAbsolutePath());
      } else {
        throw new MoeProblem(
            e,
            "Merge returned with unexpected status %d when trying to run \"merge -p %s %s %s\"",
            e.returnStatus,
            destFile.getAbsolutePath(),
            origFile.getAbsolutePath(),
            modFile.getAbsolutePath());
      }
    }
  }

  /**
   * The encapsulated results of a {@link CodebaseMerger#merge(Codebase,Codebase,Codebase)}
   * operation
   */
  @AutoValue
  public abstract static class MergeResult {
    /** The merged codebase representation. */
    public abstract Codebase mergedCodebase();
    /** Files that moe was able to successfully merge */
    public abstract ImmutableSet<String> mergedFiles();
    /** Files that moe was unable to merge */
    public abstract ImmutableSet<String> failedFiles();

    /** Print the results of a merge to the UI. */
    public void report(Ui ui) {
      ui.message("Merged codebase generated at: %s", mergedCodebase().root().getAbsolutePath());
      if (failedFiles().isEmpty()) {
        ui.message("%d files merged successfully. No merge conflicts.", mergedFiles().size());
      } else {
        ui.message(
            "%d files merged successfully.\n%d files have merge "
                + "conflicts. Edit the following files to resolve conflicts:\n%s",
            mergedFiles().size(), failedFiles().size(), failedFiles());
      }
    }

    static Builder builder() {
      return new AutoValue_CodebaseMerger_MergeResult.Builder();
    }

    /** A builder for {@link CodebaseMerger.MergeResult} */
    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setMergedCodebase(Codebase mergedCodebase);

      abstract Codebase mergedCodebase();

      abstract ImmutableSet.Builder<String> failedFilesBuilder();

      abstract ImmutableSet.Builder<String> mergedFilesBuilder();

      abstract MergeResult build();
    }
  }
}
