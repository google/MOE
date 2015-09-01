// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.tools;

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


}
