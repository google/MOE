// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.Evaluator;
import com.google.devtools.moe.client.logic.DiffCodebasesLogic;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;

import org.kohsuke.args4j.Option;

/**
 * Print the diff of two Codebases.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DiffCodebasesDirective implements Directive {

  private final DiffCodebasesOptions options = new DiffCodebasesOptions();

  public DiffCodebasesDirective() {}

  @Override
  public DiffCodebasesOptions getFlags() {
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

    Codebase codebase1;
    try {
      codebase1 = Evaluator.parseAndEvaluate(options.codebase1, context);
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error(String.format("Error creating codebase %s: %s",
                                            options.codebase1, e.getMessage()));
      return 1;
    }

    Codebase codebase2;
    try {
      codebase2 = Evaluator.parseAndEvaluate(options.codebase2, context);
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error(String.format("Error creating codebase %s: %s",
                                            options.codebase2, e.getMessage()));
      return 1;
    }

    DiffCodebasesLogic.printDiff(codebase1, codebase2);
    return 0;
  }

  static class DiffCodebasesOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--codebase1", required = true,
            usage = "Codebase1 expression")
    String codebase1 = "";
    @Option(name = "--codebase2", required = true,
            usage = "Codebase2 expression")
    String codebase2 = "";
  }

}
