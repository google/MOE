// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

/**
 * A problem that we do not expect to routinely happen. They should end execution of MOE and require
 * intervention by moe-team.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class MoeProblem extends RuntimeException {
  // https://www.youtube.com/watch?v=xZ4tNmnuMgQ

  // TODO(cgruber): Figure out why this is public mutable and fix.
  public String explanation;
  private final Object[] args;

  // TODO(cgruber): Check not null and ensure no one is calling it that way.
  public MoeProblem(String explanation, Object... args) {
    this.explanation = explanation;
    this.args = args;
  }

  @Override
  public String getMessage() {
    return String.format(explanation, args);
  }
}
