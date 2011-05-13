// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import org.kohsuke.args4j.Option;

/**
 * Standard options for all MOE directives.
 * Directives should subclass this if they want directive-specific options.
 * @author nicksantos@google.com (Nick Santos)
 */
public class MoeOptions {
  @Option(name = "--help",
      handler = BooleanOptionHandler.class,
      usage = "Prints available flags for this directive.")
  private boolean help = false;

  public boolean shouldDisplayHelp() {
    return help;
  }
}
