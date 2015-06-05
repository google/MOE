// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.logic.ChangeLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Create a Change in a source control system using command line flags.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ChangeDirective extends Directive {
  private final ChangeOptions options = new ChangeOptions();

  private final ProjectContextFactory contextFactory;
  private final Ui ui;

  @Inject
  ChangeDirective(ProjectContextFactory contextFactory, Ui ui) {
    this.contextFactory = contextFactory;
    this.ui = ui;
  }

  @Override
  public ChangeOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = contextFactory.create(options.configFilename);
    } catch (InvalidProject e) {
      ui.error(e, "Error creating project");
      return 1;
    }

    Task changeTask =
        ui.pushTask(
            "create_change",
            String.format(
                "Creating a change in \"%s\" with contents \"%s\"",
                options.destination,
                options.codebase));

    Codebase c;
    try {
      c = Parser.parseExpression(options.codebase).createCodebase(context);
    } catch (ParseError e) {
      ui.error(e, "Error parsing codebase");
      return 1;
    } catch (CodebaseCreationError e) {
      ui.error(e, "Error creating codebase");
      return 1;
    }

    Writer destination;
    try {
      destination = Parser.parseRepositoryExpression(options.destination).createWriter(context);
    } catch (ParseError e) {
      ui.error(e, "Error parsing change destination");
      return 1;
    } catch (WritingError e) {
      ui.error(e, "Error writing change");
      return 1;
    }

    DraftRevision r = ChangeLogic.change(c, destination);
    if (r == null) {
      return 1;
    }

    ui.popTaskAndPersist(changeTask, destination.getRoot());
    return 0;
  }

  @Override
  public String getDescription() {
    return "Creates a (pending) change";
  }

  static class ChangeOptions extends MoeOptions {

    @Option(name = "--config_file", required = true, usage = "Location of MOE config file")
    String configFilename = "";

    @Option(name = "--codebase", required = true, usage = "Codebase expression to evaluate")
    String codebase = "";

    @Option(name = "--destination", required = true, usage = "Expression of destination writer")
    String destination = "";
  }
}
