// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.Evaluator;
import com.google.devtools.moe.client.logic.ChangeLogic;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterEvaluator;
import com.google.devtools.moe.client.writer.WritingError;

import org.kohsuke.args4j.Option;

/**
 * Create a Change in a source control system using command line flags.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ChangeDirective implements Directive {

  private final ChangeOptions options = new ChangeOptions();

  public ChangeDirective() {}

  public ChangeOptions getFlags() {
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

    if (options.codebase.isEmpty()) {
      AppContext.RUN.ui.error("No --codebase specified.");
      return 1;
    }

    if (options.destination.isEmpty()) {
      AppContext.RUN.ui.error("No --destination specified.");
      return 1;
    }

    AppContext.RUN.ui.info(
        String.format("Creating a change in \"%s\" with contents \"%s\"",
                      options.destination, options.codebase));

    Codebase c;
    try {
      c = Evaluator.parseAndEvaluate(options.codebase, context);
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }

    Writer destination;
    try {
      destination = WriterEvaluator.parseAndEvaluate(options.destination, context);
    } catch (WritingError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }

    DraftRevision r = ChangeLogic.change(c, destination);
    if (r == null) {
      return 1;
    }

    AppContext.RUN.ui.info("Created Draft Revision: " + r.getLocation());
    return 0;
  }

  static class ChangeOptions extends MoeOptions {
    @Option(name = "--config_file",
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--codebase",
            usage = "Codebase expression to evaluate")
    String codebase = "";
    @Option(name = "--destination",
            usage = "Expression of destination writer")
    String destination = "";
  }

}
