// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import org.kohsuke.args4j.Option;

/**
 * Standard options for all MOE directives.
 * Tasks should subclass this if they want task-specific options.
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

  // Here is where accessors for options common across Tasks go.
  // e.g., getConfigFile

  // TODO(dbentley): delete this once we have converted.
  public MoeOptions() {}
}
