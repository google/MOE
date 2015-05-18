// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.logic.MergeCodebasesLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Merge three codebases into one.
 *
 * See MergeCodebasesLogic.merge() for a more detailed description.
 *
 */
public class MergeCodebasesDirective extends Directive {
  private final MergeCodebasesOptions options = new MergeCodebasesOptions();

  private final ProjectContextFactory contextFactory;
  private final Ui ui;

  @Inject
  MergeCodebasesDirective(ProjectContextFactory contextFactory, Ui ui) {
    this.contextFactory = contextFactory;
    this.ui = ui;
  }

  @Override
  public MoeOptions getFlags() {
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

    Codebase originalCodebase, destinationCodebase, modifiedCodebase;
    try {
      originalCodebase = Parser.parseExpression(options.originalCodebase).createCodebase(context);
      modifiedCodebase = Parser.parseExpression(options.modifiedCodebase).createCodebase(context);
      destinationCodebase =
          Parser.parseExpression(options.destinationCodebase).createCodebase(context);
    } catch (ParseError e) {
      ui.error(e, "Error parsing");
      return 1;
    } catch (CodebaseCreationError e) {
      ui.error(e, "Error creating codebase");
      return 1;
    }
    Codebase mergedCodebase = MergeCodebasesLogic.merge(originalCodebase, destinationCodebase,
        modifiedCodebase);

    return 0;
  }

  @Override
  public String getDescription() {
    return "Merges three codebases into a new codebase";
  }

  static class MergeCodebasesOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--original_codebase", required = true,
            usage = "Codebase expression for the original repository")
    String originalCodebase = "";
    @Option(name = "--modified_codebase", required = true,
      usage = "Codebase expression for the modified repository")
    String modifiedCodebase = "";
    @Option(name = "--destination_codebase", required = true,
            usage = "Codebase expression for the destination repository")
    String destinationCodebase = "";
  }

}
