// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeOptions;
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

  @Override
  public CheckConfigOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    try {
      ProjectContext context = AppContext.RUN.contextFactory.makeProjectContext(
          options.configFilename);
      return 0;
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e, "Invalid project");
      return 1;
    }
  }

  static class CheckConfigOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
        usage = "Location of MOE config file")
    String configFilename = "";
  }
}
