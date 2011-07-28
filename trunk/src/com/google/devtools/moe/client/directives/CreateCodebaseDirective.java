// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.Evaluator;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;


import org.kohsuke.args4j.Option;

/**
 * Print the head revision of a repository.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CreateCodebaseDirective implements Directive {

  private final CreateCodebaseOptions options = new CreateCodebaseOptions();

  public CreateCodebaseDirective() {}

  @Override
  public CreateCodebaseOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e.explanation);
      return 1;
    }

    Codebase c;
    try {
      c = Evaluator.parseAndEvaluate(options.codebase, context);
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error("Error creating codebase: " + e.getMessage());
      return 1;
    }
    AppContext.RUN.ui.info(
        String.format("Codebase \"%s\" created at %s", c.toString(), c.getPath()));
    return 0;
  }

  static class CreateCodebaseOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--codebase", required = true,
            usage = "Codebase expression to evaluate")
    String codebase = "";
  }

}
