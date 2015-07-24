// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.logic.DiffCodebasesLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.project.ProjectContextFactory;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Print the diff of two Codebases.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DiffCodebasesDirective extends Directive {
  @Option(name = "--codebase1", required = true, usage = "Codebase1 expression")
  String codebase1Spec = "";

  @Option(name = "--codebase2", required = true, usage = "Codebase2 expression")
  String codebase2Spec = "";

  private final Ui ui;

  @Inject
  DiffCodebasesDirective(ProjectContextFactory contextFactory, Ui ui) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    Codebase codebase1, codebase2;
    try {
      codebase1 = Parser.parseExpression(codebase1Spec).createCodebase(context());
      codebase2 = Parser.parseExpression(codebase2Spec).createCodebase(context());
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
}
