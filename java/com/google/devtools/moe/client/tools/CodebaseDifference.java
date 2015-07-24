// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.tools;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.codebase.Codebase;

import java.util.Collections;
import java.util.Set;

/**
 * Describes the difference between two Codebases.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CodebaseDifference {

  public final Codebase codebase1;
  public final Codebase codebase2;
  public final Set<FileDifference> fileDiffs;

  public CodebaseDifference(Codebase codebase1, Codebase codebase2, Set<FileDifference> fileDiffs) {
    this.codebase1 = codebase1;
    this.codebase2 = codebase2;
    this.fileDiffs = Collections.unmodifiableSet(fileDiffs);
  }

  /**
   * Return whether the Codebases are different.
   */
  public boolean areDifferent() {
    return !this.fileDiffs.isEmpty();
  }

  /**
   * Diff two Codebases.
   */
  public static CodebaseDifference diffCodebases(Codebase codebase1, Codebase codebase2) {
    return diffCodebases(codebase1, codebase2, FileDifference.CONCRETE_FILE_DIFFER);
  }

  /**
   * Diff two Codebases with a custom FileDiffer.
   */
  public static CodebaseDifference diffCodebases(
      Codebase codebase1, Codebase codebase2, FileDifference.FileDiffer differ) {
    Set<String> filenames =
        Sets.union(codebase1.getRelativeFilenames(), codebase2.getRelativeFilenames());

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
