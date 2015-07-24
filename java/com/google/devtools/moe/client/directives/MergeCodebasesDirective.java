// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.logic.MergeCodebasesLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
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
  @Option(
    name = "--original_codebase",
    required = true,
    usage = "Codebase expression for the original repository"
  )
  String originalExpression = "";

  @Option(
    name = "--modified_codebase",
    required = true,
    usage = "Codebase expression for the modified repository"
  )
  String modifiedExpression = "";

  @Option(
    name = "--destination_codebase",
    required = true,
    usage = "Codebase expression for the destination repository"
  )
  String destinationExpression = "";

  private final Ui ui;

  @Inject
  MergeCodebasesDirective(ProjectContextFactory contextFactory, Ui ui) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    Codebase originalCodebase, destinationCodebase, modifiedCodebase;
    try {
      originalCodebase = Parser.parseExpression(originalExpression).createCodebase(context());
      modifiedCodebase = Parser.parseExpression(modifiedExpression).createCodebase(context());
      destinationCodebase = Parser.parseExpression(destinationExpression).createCodebase(context());
    } catch (ParseError e) {
      ui.error(e, "Error parsing");
      return 1;
    } catch (CodebaseCreationError e) {
      ui.error(e, "Error creating codebase");
      return 1;
    }
    MergeCodebasesLogic.merge(originalCodebase, destinationCodebase, modifiedCodebase);
    return 0;
  }

  @Override
  public String getDescription() {
    return "Merges three codebases into a new codebase";
  }
}
