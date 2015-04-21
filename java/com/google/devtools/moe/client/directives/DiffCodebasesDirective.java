// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.logic.DiffCodebasesLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Print the diff of two Codebases.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DiffCodebasesDirective extends Directive {
  private final DiffCodebasesOptions options = new DiffCodebasesOptions();

  private final ProjectContextFactory contextFactory;
  private final Ui ui;

  @Inject
  DiffCodebasesDirective(ProjectContextFactory contextFactory, Ui ui) {
    this.contextFactory = contextFactory;
    this.ui = ui;
  }

  @Override
  public DiffCodebasesOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      ui.error(e, "Error creating project");
      return 1;
    }

    Codebase codebase1, codebase2;
    try {
      codebase1 = Parser.parseExpression(options.codebase1).createCodebase(context);
      codebase2 = Parser.parseExpression(options.codebase2).createCodebase(context);
    } catch (ParseError e) {
      ui.error(e, "Error parsing codebase expression");
      return 1;
    } catch (CodebaseCreationError e) {
      ui.error(e, "Error creating codebase");
      return 1;
    }

    DiffCodebasesLogic.printDiff(codebase1, codebase2);
    return 0;
  }

  @Override
  public String getDescription() {
    return "Prints the diff output between two codebase expressions";
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
