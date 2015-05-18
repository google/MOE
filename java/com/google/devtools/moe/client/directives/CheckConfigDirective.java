// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContextFactory;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Reads a MOE Project's configuration and reads it, checking for errors.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CheckConfigDirective extends Directive {
  private final CheckConfigOptions options = new CheckConfigOptions();

  private final ProjectContextFactory contextFactory;
  private final Ui ui;

  @Inject
  CheckConfigDirective(ProjectContextFactory contextFactory, Ui ui) {
    this.contextFactory = contextFactory;
    this.ui = ui;
  }

  @Override
  public CheckConfigOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    try {
      contextFactory.create(options.configFilename);
      return 0;
    } catch (InvalidProject e) {
      ui.error(e, "Invalid project");
      return 1;
    }
  }

  @Override
  public String getDescription() {
    return "Checks that the project's configuration is valid";
  }

  static class CheckConfigOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
        usage = "Location of MOE config file")
    String configFilename = "";
  }
}
