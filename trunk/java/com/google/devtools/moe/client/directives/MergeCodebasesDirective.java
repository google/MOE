// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.Evaluator;
import com.google.devtools.moe.client.logic.MergeCodebasesLogic;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;

import org.kohsuke.args4j.Option;

/**
 * Merge three codebases into one.
 *
 * See MergeCodebasesLogic.merge() for a more detailed description.
 *
 */
public class MergeCodebasesDirective implements Directive {

  private final MergeCodebasesOptions options = new MergeCodebasesOptions();

  @Override
  public MoeOptions getFlags() {
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

    Codebase originalCodebase, destinationCodebase, modifiedCodebase;
    try {
      originalCodebase = Evaluator.parseAndEvaluate(options.originalCodebase, context);
      modifiedCodebase = Evaluator.parseAndEvaluate(options.modifiedCodebase, context);
      destinationCodebase = Evaluator.parseAndEvaluate(options.destinationCodebase, context);
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }
    Codebase mergedCodebase = MergeCodebasesLogic.merge(originalCodebase, destinationCodebase,
        modifiedCodebase);

    return 0;
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
