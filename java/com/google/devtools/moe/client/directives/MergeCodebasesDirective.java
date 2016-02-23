/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseMerger;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Merge three codebases into one.
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

  private final Lazy<ProjectContext> context;
  private final FileDiffer differ;
  private final Ui ui;
  private final FileSystem filesystem;
  private final CommandRunner cmd;

  @Inject
  MergeCodebasesDirective(
      Lazy<ProjectContext> context,
      FileDiffer differ,
      Ui ui,
      FileSystem filesystem,
      CommandRunner cmd) {
    this.context = context;
    this.differ = differ;
    this.ui = ui;
    this.filesystem = filesystem;
    this.cmd = cmd;
  }

  @Override
  protected int performDirectiveBehavior() {
    Codebase originalCodebase, destinationCodebase, modifiedCodebase;
    try {
      originalCodebase = Parser.parseExpression(originalExpression).createCodebase(context.get());
      modifiedCodebase = Parser.parseExpression(modifiedExpression).createCodebase(context.get());
      destinationCodebase =
          Parser.parseExpression(destinationExpression).createCodebase(context.get());
    } catch (ParseError e) {
      throw new MoeProblem(e, "Error parsing");
    } catch (CodebaseCreationError e) {
      throw new MoeProblem(e, "Error creating codebase");
    }
    new CodebaseMerger(
            ui, filesystem, cmd, differ, originalCodebase, destinationCodebase, modifiedCodebase)
        .merge();
    return 0;
  }

  @Override
  public String getDescription() {
    return "Merges three codebases into a new codebase";
  }
}
