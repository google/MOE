// Copyright 2015 The MOE Authors All Rights Reserved.
package com.google.devtools.moe.client.tools;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;

import java.util.Set;

import javax.inject.Inject;

/** Performs a difference analysis using an underlying {@code FileDiffer}. */
public class CodebaseDiffer {
  private final FileDiffer differ;

  @Inject
  public CodebaseDiffer(FileDiffer differ) {
    this.differ = differ;
  }

  /**
   * Diff two {@link Codebase} instances with a {@link FileDiffer}.
   */
  public CodebaseDifference diffCodebases(Codebase codebase1, Codebase codebase2) {
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
