// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;

import org.kohsuke.args4j.Option;

/**
 * Reads a MOE Project's configuration and reads it, checking for errors.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CheckConfigDirective implements Directive {

  private final CheckConfigOptions options = new CheckConfigOptions();

  public CheckConfigDirective() {}

  public CheckConfigOptions getFlags() {
    return options;
  }

  public int perform() {
    if (options.configFilename.isEmpty()) {
      System.err.println("No --config_file specified.");
      return 1;
    }
    try {
      ProjectContext context = AppContext.RUN.contextFactory.makeProjectContext(
          options.configFilename);
      return 0;
    } catch (InvalidProject e) {
      System.err.println("Invalid project: " + e.explanation);
      return 1;
    }
  }

  static class CheckConfigOptions extends MoeOptions {
    @Option(name = "--config_file",
        usage = "Location of MOE config file")
    String configFilename = "";
  }
}
