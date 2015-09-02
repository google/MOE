// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.options;

import org.kohsuke.args4j.Option;

/**
 * Standard options for all MOE directives.
 * Tasks should subclass this if they want task-specific options.
 *
 * @author nicksantos@google.com (Nick Santos)
 */
// TODO(cgruber) remove extends when JCommander is used.
public abstract class MoeOptions extends DebugOptions {
  @Option(name = "--config_file", required = true, usage = "Location of MOE config file")
  protected String configFilename = "";

  @Option(
    name = "--help",
    handler = BooleanOptionHandler.class,
    usage = "Prints available flags for this directive."
  )
  private boolean help = false;

  public boolean shouldDisplayHelp() {
    return help;
  }
}
