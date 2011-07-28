// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.tools.CodebaseDifference;
import com.google.devtools.moe.client.tools.PatchCodebaseDifferenceRenderer;

/**
 * Performs the logic of the DiffCodebasesDirective
 *
 */
public class DiffCodebasesLogic {

  /**
   * Prints the diff or lack thereof of the two codebases.
   *
   * @param c1 the Codebase to diff with c2
   * @param c2 the Codebase to diff with c1
   */
  public static void printDiff(Codebase c1, Codebase c2) {
    CodebaseDifference diff = CodebaseDifference.diffCodebases(c1, c2);

    if (diff.areDifferent()) {
      AppContext.RUN.ui.info(
          String.format("Codebases \"%s\" and \"%s\" differ:\n%s",
                        c1.toString(), c2.toString(),
                        new PatchCodebaseDifferenceRenderer().render(diff)));
    } else {
      AppContext.RUN.ui.info(
          String.format("Codebases \"%s\" and \"%s\" are identical",
                        c1.toString(), c2.toString()));
    }
  }
}
