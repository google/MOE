// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.tools.FileDifference;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Once constructed with three codebases, this class can call merge() which will merge the three
 * codebases into one. A CodebaseMerger keeps track of the names of files that either merged
 * successfully or conflicted.
 *
 */
public class CodebaseMerger {

  private final Codebase originalCodebase, destinationCodebase, modifiedCodebase, mergedCodebase;
  private final Set<String> mergedFiles, failedToMergeFiles;

  public CodebaseMerger(Codebase originalCodebase, Codebase modifiedCodebase,
      Codebase destinationCodebase) {
    this.originalCodebase = originalCodebase;
    this.modifiedCodebase = modifiedCodebase;
    this.destinationCodebase = destinationCodebase;

    File mergedDir = AppContext.RUN.fileSystem.getTemporaryDirectory("merged_codebase_");
    CodebaseExpression mergedExpression = new CodebaseExpression(
        new Term("merged", ImmutableMap.<String, String>of()));
    this.mergedCodebase = new Codebase(mergedDir, "merged", mergedExpression);

    mergedFiles = Sets.newHashSet();
    failedToMergeFiles = Sets.newHashSet();
  }

  public Set<String> getMergedFiles() {
    return ImmutableSet.copyOf(mergedFiles);
  }

  public Set<String> getFailedToMergeFiles() {
    return ImmutableSet.copyOf(failedToMergeFiles);
  }

  /**
   * For each file in the union of the modified and destination codebases, run
   * generateMergedFile(...) and then report() the results.
   *
   * @return the merged Codebase
   */
  public Codebase merge() {
    Set<String>  filesToMerge = Sets.union(destinationCodebase.getRelativeFilenames(),
        modifiedCodebase.getRelativeFilenames());
    for (String filename : filesToMerge) {
      this.generateMergedFile(filename);
    }
    this.report();
    return mergedCodebase;
  }

  /**
   * Print the results of a merge to the UI.
   */
  public void report() {
    AppContext.RUN.ui.info(String.format("Merged codebase generated at: %s",
        mergedCodebase.getPath().getAbsolutePath()));
    AppContext.RUN.ui.info(String.format("%d files merged successfully\n%d files have merge "
        + "conflicts. Edit the following files to resolve conflicts:\n%s", mergedFiles.size(),
        failedToMergeFiles.size(), failedToMergeFiles.toString()));
  }

  /**
   * Given a filename, this method finds the file with that name in each of the three codebases.
   * Using the UNIX merge(1) tool, those three files are merged and the result is placed in the
   * merged codebase. Any conflicts that occurred during merging will appear in the merged codebase
   * file for the user to resolve.
   *
   * In the case where the file specified by the given filename exists in the original codebase and
   * in either the modified codebase or the destination codebase (but not both) and if the file
   * is unchanged between those codebases, then a file in the merged codebase will NOT be created
   * and this method will return leaving the merged codebase unchanged.
   *
   * @param filename the name of the file to merge
   */
  public void generateMergedFile(String filename) {
    FileSystem fs = AppContext.RUN.fileSystem;

    // Check that the files exist in their respective repositories. If not, assign them the null
    // device.
    File origFile = originalCodebase.getFile(filename);
    boolean origExists = fs.exists(origFile);
    if (!origExists) {
      origFile = new File("/dev/null");
    }

    File destFile = destinationCodebase.getFile(filename);
    boolean destExists = fs.exists(destFile);
    if (!destExists) {
      destFile = new File("/dev/null");
    }

    File modFile = modifiedCodebase.getFile(filename);
    boolean modExists = fs.exists(modFile);
    if (!modExists) {
      modFile = new File("/dev/null");
    }

    if (!destExists && !modExists) {
      // This should never be thrown since generateMergedFile(...) is only called on filesToMerge
      // from merge() which is the union of the files in the destination and modified codebases.
      throw new MoeProblem(
          String.format("%s doesn't exist in either %s nor %s. This should not be possible.",
          filename, destinationCodebase, modifiedCodebase));
    }

    // The file no longer exists in either the modified or destination codebases.
    if (origExists && !(destExists && modExists)) {
      // Figure out which one still exists.
      File existingFile = (destExists) ? destFile : modFile;
      // If the file is unchanged from the original in either the modified or destination, defer
      // to its deletion in the other. Otherwise we can continue since a merge conflict will occur
      // which is what we want to happen.
      if (FileDifference.CONCRETE_FILE_DIFFER.diffFiles(filename, origFile, existingFile) == null) {
        return;
      }
    }

    // Copy the destFile into the merged codebase. This is where the output of merge will be
    // written to.
    File mergedFile = mergedCodebase.getFile(filename);
    try {
      fs.makeDirsForFile(mergedFile);
      fs.copyFile(destFile, mergedFile);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }

    String mergeOutput;
    try {
      // Merges the changes that lead from origFile to modFile into mergedFile (which is a copy
      // of destFile). After, mergedFile will have the combined changes of modFile and destFile.
      mergeOutput = AppContext.RUN.cmd.runCommand("merge", ImmutableList.of(
          mergedFile.getAbsolutePath(), origFile.getAbsolutePath(), modFile.getAbsolutePath()), "",
          this.mergedCodebase.getPath().getAbsolutePath());
      // Return status was 0 and the merge was successful. Note it.
      mergedFiles.add(mergedFile.getAbsolutePath().toString());
    } catch (CommandException e) {
      // If merge fails with exit status 1, then a conflict occurred. Make a note of the filepath.
      if (e.returnStatus == 1) {
        failedToMergeFiles.add(mergedFile.getAbsolutePath().toString());
      } else {
        throw new MoeProblem(
            String.format("Merge returned with unexpected status %d when trying to run "
            + "\"merge -p %s %s %s\"", e.returnStatus, destFile.getAbsolutePath(),
            origFile.getAbsolutePath(), modFile.getAbsolutePath()));
      }
    }
  }
}
