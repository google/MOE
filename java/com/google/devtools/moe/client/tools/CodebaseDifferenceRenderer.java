// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.tools;

/**
 * Interface for rendering differences between Codebases.
 *
 * This allows one implementation that creates an applicable patch, and one that summarizes
 * the differences without scrolling off your screen.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface CodebaseDifferenceRenderer {

  /**
   * Render the difference.
   */
  public String render(CodebaseDifference diff);
}
