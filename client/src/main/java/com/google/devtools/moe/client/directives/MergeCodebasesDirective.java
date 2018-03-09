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

import static com.google.devtools.moe.client.codebase.expressions.Parser.parseExpression;

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseMerger;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.codebase.expressions.Parser.ParseError;
import com.google.devtools.moe.client.project.ProjectContext;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.inject.Inject;
import org.kohsuke.args4j.Option;

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

  private final ProjectContext context;

  private final ExpressionEngine expressionEngine;
  private final CodebaseMerger merger;

  @Inject
  MergeCodebasesDirective(
      ProjectContext context, ExpressionEngine expressionEngine, CodebaseMerger merger) {
    this.context = context;
    this.expressionEngine = expressionEngine;
    this.merger = merger;
  }

  @Override
  protected int performDirectiveBehavior() {
    Codebase originalCodebase;
    Codebase modifiedCodebase;
    Codebase destinationCodebase;
    try {
      originalCodebase =
          expressionEngine.createCodebase(parseExpression(originalExpression), context);
      modifiedCodebase =
          expressionEngine.createCodebase(parseExpression(modifiedExpression), context);
      destinationCodebase =
          expressionEngine.createCodebase(parseExpression(destinationExpression), context);
    } catch (ParseError e) {
      throw new MoeProblem(e, "Error parsing");
    } catch (CodebaseCreationError e) {
      throw new MoeProblem(e, "Error creating codebase");
    }
    merger.merge(originalCodebase, modifiedCodebase, destinationCodebase);
    return 0;
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<MergeCodebasesDirective> {
    private static final String COMMAND = "merge_codebases";

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public Directive directive(MergeCodebasesDirective directive) {
      return directive;
    }

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public String description() {
      return "Merges three codebases into a new codebase";
    }
  }
}
