// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

/**
 * A problem that we do not expect to routinely happen. They should end execution of MOE and require
 * intervention by moe-team.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class MoeProblem extends RuntimeException {

  public String explanation;

  public MoeProblem(String explanation) {
    this.explanation = explanation;
  }

  @Override
  public String getMessage() { return explanation; }

}
