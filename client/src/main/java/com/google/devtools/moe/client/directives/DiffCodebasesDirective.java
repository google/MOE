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

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.codebase.expressions.Parser;
import com.google.devtools.moe.client.codebase.expressions.Parser.ParseError;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.tools.CodebaseDiffer;
import com.google.devtools.moe.client.tools.CodebaseDifference;
import com.google.devtools.moe.client.tools.PatchCodebaseDifferenceRenderer;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;
import javax.inject.Inject;
import org.kohsuke.args4j.Option;

/**
 * Print the diff of two Codebases.
 */
public class DiffCodebasesDirective extends Directive {
  private static final PatchCodebaseDifferenceRenderer RENDERER =
      new PatchCodebaseDifferenceRenderer();

  @Option(name = "--codebase1", required = true, usage = "Codebase1 expression")
  String codebase1Spec = "";

  @Option(name = "--codebase2", required = true, usage = "Codebase2 expression")
  String codebase2Spec = "";

  private final ProjectContext context;
  private final CodebaseDiffer differ;
  private final Ui ui;
  private final ExpressionEngine expressionEngine;

  @Inject
  DiffCodebasesDirective(
      ProjectContext context, CodebaseDiffer differ, Ui ui, ExpressionEngine expressionEngine) {
    this.context = context;
    this.differ = differ;
    this.ui = ui;
    this.expressionEngine = expressionEngine;
  }

  @Override
  protected int performDirectiveBehavior() {
    Codebase codebase1;
    Codebase codebase2;
    try {
      codebase1 = expressionEngine.createCodebase(Parser.parseExpression(codebase1Spec), context);
      codebase2 = expressionEngine.createCodebase(Parser.parseExpression(codebase2Spec), context);
    } catch (ParseError e) {
      throw new MoeProblem(e, "Error parsing codebase expression");
    } catch (CodebaseCreationError e) {
      throw new MoeProblem(e, "Error creating codebase");
    }

    CodebaseDifference diff = differ.diffCodebases(codebase1, codebase2);
    if (diff.areDifferent()) {
      ui.message(
          "Codebases \"%s\" and \"%s\" differ:\n%s", codebase1, codebase2, RENDERER.render(diff));
    } else {
      ui.message("Codebases \"%s\" and \"%s\" are identical", codebase1, codebase2);
    }
    return 0;
  }

  /**
   * A module to supply the directive and a description into maps in the graph.
   */
  @dagger.Module
  public static class Module implements Directive.Module<DiffCodebasesDirective> {
    private static final String COMMAND = "diff_codebases";

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public Directive directive(DiffCodebasesDirective directive) {
      return directive;
    }

    @Override
    @Provides
    @IntoMap
    @StringKey(COMMAND)
    public String description() {
      return "Prints the diff output between two codebase expressions";
    }
  }
}
