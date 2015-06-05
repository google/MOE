// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseMerger;

/**
 * Provides a static method merge(...) that can be called from the MergeCodebasesDirective or from
 * another class.
 *
 */
public class MergeCodebasesLogic {

  /**
   * Incorporates all changes that lead from originalCodebase to modifiedCodebase into
   * destinationCodebase.
   *
   * Here is a description of the UNIX merge(1) tool from its man page:
   *
   *   merge [ options ] file1 file2 file3
   *
   *   merge incorporates all changes that lead from file2 to file3 into file1.  The result
   *   ordinarily goes into file1.  merge is useful for combining separate changes to an original.
   *   Suppose file2 is the original, and both file1 and file3 are modifications of file2.  Then
   *   merge combines both changes.
   *
   * MergeCodebasesLogic.merge performs this type of merge on each file in the three codebases. In
   * MergeCodebasesLogic.merge, originalCodebase is analogous to file2, modifiedCodebase is
   * analogous to file3, and destinationCodebase is analogous to file1. The output of
   * MergeCodebasesLogic.merge is a codebase that incorporates the changes that both
   * modifiedCodebase and destinationCodebase made on the originalCodebase. The differences between
   * modifiedCodebase and the originalCodebase are brought into a copy of destinationCodebase. The
   * result is the merged codebase.
   *
   * This is useful when bringing changes to the public repository into the internal repository.
   * For example, say you run:
   *
   *    merge_codebases --originalCodebase "publicrepo(revision=142)"
   *                    --modifiedCodebase "publicrepo(revision=143)"
   *                    --destinationCodebase "internalrepo(revision=74)"
   *
   * Let internalrepo(revision=74) be in equivalence with publicrepo(revision=142). That is, let
   * publicrepo(revision=142) represent the same state of the code as internalrepo(revision=74)
   * minus any confidential code that may have been scrubbed during translation. That means that
   * publicrepo(revision=143) is a change to the public repository which has yet to be brought to
   * the internal repository. By running the above merge_codebases, the changes from the public
   * revision 142 to 143 will be merged into a copy of internal revision 74. The result is an
   * internal revision 75 which has the new public changes and still has the confidential code that
   * a public revision wouldn't have. Thus, internal revision 75 would be equivalent with public
   * revision 143 assuming there were no conflicts when merging.
   *
   * @param originalCodebase a Codebase, a set of files
   * @param modifiedCodebase a Codebase that represents a modified version of originalCodebase
   * @param destinationCodebase a Codebase in which to combine the changes
   *
   * @return the Codebase that results from combining modifiedCodebase's modifications to
   * the originalCodebase into a copy of the destinationCodebase
   */
  public static Codebase merge(
      Codebase originalCodebase, Codebase modifiedCodebase, Codebase destinationCodebase) {
    CodebaseMerger merger =
        new CodebaseMerger(originalCodebase, modifiedCodebase, destinationCodebase);
    return merger.merge();
  }
}
