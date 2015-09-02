// Copyright 2015 The MOE Authors All Rights Reserved.
package com.google.devtools.moe.client.options;

import org.kohsuke.args4j.Option;

/**
 * Options/Flag class to hold the {@code --debug} option
 */
public class DebugOptions {
  @Option(name = "--debug", handler = BooleanOptionHandler.class, usage = "Logs debug information.")
  boolean debug = false;

  public boolean debug() {
    return debug;
  }
}
