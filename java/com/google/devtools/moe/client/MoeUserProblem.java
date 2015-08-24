// Copyright 2015 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

/**
 * A problem that we expect to routinely happen, and which should be reported cleanly to the
 * CLI upon catching.
 */
public abstract class MoeUserProblem extends RuntimeException {
  public MoeUserProblem() {}

  /**
   * A method which allows the user-visible message to be reported appropriately to the
   * {@link Ui} class.  Implementers should override this message and log any user output
   * relevant to the error.
   */
  public abstract void reportTo(Messenger ui);
}