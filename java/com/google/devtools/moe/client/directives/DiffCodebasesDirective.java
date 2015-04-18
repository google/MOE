// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.logic.DiffCodebasesLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
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
      context = Injector.INSTANCE.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      Injector.INSTANCE.ui.error(e, "Error creating project");
      return 1;
    }

    Codebase codebase1, codebase2;
    try {
      codebase1 = Parser.parseExpression(options.codebase1).createCodebase(context);
      codebase2 = Parser.parseExpression(options.codebase2).createCodebase(context);
    } catch (ParseError e) {
      Injector.INSTANCE.ui.error(e, "Error parsing codebase expression");
      return 1;
    } catch (CodebaseCreationError e) {
      Injector.INSTANCE.ui.error(e, "Error creating codebase");
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
