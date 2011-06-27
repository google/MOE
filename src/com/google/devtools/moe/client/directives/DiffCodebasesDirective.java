// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.Evaluator;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.tools.CodebaseDifference;
import com.google.devtools.moe.client.tools.PatchCodebaseDifferenceRenderer;

import org.kohsuke.args4j.Option;

/**
 * Print the head revision of a repository.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DiffCodebasesDirective implements Directive {

  private final DiffCodebasesOptions options = new DiffCodebasesOptions();

  public DiffCodebasesDirective() {}

  public DiffCodebasesOptions getFlags() {
    return options;
  }

  public int perform() {
    ProjectContext context;
    if (options.configFilename.isEmpty()) {
      AppContext.RUN.ui.error("No --config_file specified.");
      return 1;
    }
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e.explanation);
      return 1;
    }

    if (options.codebase1.isEmpty()) {
      AppContext.RUN.ui.error("No --codebase1 specified.");
      return 1;
    }

    if (options.codebase2.isEmpty()) {
      AppContext.RUN.ui.error("No --codebase2 specified.");
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

    CodebaseDifference diff = CodebaseDifference.diffCodebases(codebase1, codebase2);

    if (diff.areDifferent()) {
      AppContext.RUN.ui.info(
          String.format("Codebases \"%s\" and \"%s\" differ:\n%s",
                        codebase1.toString(), codebase2.toString(),
                        new PatchCodebaseDifferenceRenderer().render(diff)));
    } else {
      AppContext.RUN.ui.info(
          String.format("Codebases \"%s\" and \"%s\" are identical",
                        codebase1.toString(), codebase2.toString()));
    }
    return 0;
  }

  static class DiffCodebasesOptions extends MoeOptions {
    @Option(name = "--config_file",
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--codebase1",
            usage = "Codebase1 expression")
    String codebase1 = "";
    @Option(name = "--codebase2",
            usage = "Codebase2 expression")
    String codebase2 = "";
  }

}
