// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.logic.ChangeLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
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

  @Override
  public ChangeOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e, "Error creating project");
      return 1;
    }

    Task changeTask = AppContext.RUN.ui.pushTask(
        "create_change",
        String.format("Creating a change in \"%s\" with contents \"%s\"",
                      options.destination, options.codebase));

    Codebase c;
    try {
      c = Parser.parseExpression(options.codebase).createCodebase(context);
    } catch (ParseError e) {
      AppContext.RUN.ui.error(e, "Error parsing codebase");
      return 1;
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error(e, "Error creating codebase");
      return 1;
    }

    Writer destination;
    try {
      destination = Parser.parseRepositoryExpression(options.destination).createWriter(context);
    } catch (ParseError e) {
      AppContext.RUN.ui.error(e, "Error parsing change destination");
      return 1;
    } catch (WritingError e) {
      AppContext.RUN.ui.error(e, "Error writing change");
      return 1;
    }

    DraftRevision r = ChangeLogic.change(c, destination);
    if (r == null) {
      return 1;
    }

    AppContext.RUN.ui.popTaskAndPersist(changeTask, destination.getRoot());
    return 0;
  }

  static class ChangeOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--codebase", required = true,
            usage = "Codebase expression to evaluate")
    String codebase = "";
    @Option(name = "--destination", required = true,
            usage = "Expression of destination writer")
    String destination = "";
  }
}
