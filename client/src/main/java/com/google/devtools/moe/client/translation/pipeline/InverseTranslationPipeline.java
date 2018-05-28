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

package com.google.devtools.moe.client.translation.pipeline;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.codebase.expressions.Expression;
import com.google.devtools.moe.client.codebase.expressions.Parser;
import com.google.devtools.moe.client.codebase.expressions.Parser.ParseError;
import com.google.devtools.moe.client.project.ProjectContext;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * A Translator that translates a Codebase from one project space to another by merging the
 * Codebase with each step of forward-translation in reverse, via inverse editors.
 *
 * <p>For example, say there is a forward translator from project space "internal" to "public" with
 * two steps, renamer then scrubber. We have repositories internal(x) == public(y) (where x and y
 * are revision numbers). To inverse-translate a change public(y+1) from "public" to
 * "internal", first build a stack of each step in forward translation:
 *
 * <ul>
 * <li>internal(x) -- bottom of stack
 * <li>internal(x)|renamer
 * <li>internal(x)|renamer|scrubber -- top of stack
 * </ul>
 *
 * <p>Then onto each element at the top of the stack, merge 1) the next element in the stack
 * (to undo that forward-translation step), and 2) the input codebase. In our example, the first
 * merge, inverse scrubbing, looks like this:
 *
 * <pre>
 *          internal(x)|renamer|scrubber == public(y)
 *              /                                \
 *   internal(x)|renamer                      public(y+1)
 *                   \                       /
 *                     internal(x+1)|renamer
 * </pre>
 *
 * <p>The output of this merge should be the combined changes of un-scrubbing and revision y+1.
 * Note it is the inverse scrubber's job to "merge" correctly (a conceptual, not necessarily literal
 * merge).
 *
 * <p>The next and last step is inverse renaming:
 *
 * <pre>
 *            internal(x)|renamer
 *           /                   \
 *      internal(x)      internal(x+1)|renamer
 *           \                   /
 *               internal(x+1)
 * </pre>
 *
 * <p>We call the codebase being merged onto the "reference from-codebase". The codebase beside
 * the input being merged in is called the "reference to-codebase". In these diamonds,
 * the ref. from-codebase is the top, and the ref. to-codebase is the left.
 */
public class InverseTranslationPipeline implements TranslationPipeline {

  private final Ui ui;
  private final ExpressionEngine expressionEngine;
  private final List<TranslationStep> forwardSteps;
  private final List<InverseTranslationStep> inverseSteps;

  public InverseTranslationPipeline(
      Ui ui,
      ExpressionEngine expressionEngine,
      List<TranslationStep> forwardSteps,
      List<InverseTranslationStep> inverseSteps) {
    this.ui = ui;
    this.expressionEngine = expressionEngine;
    Preconditions.checkArgument(!inverseSteps.isEmpty());
    Preconditions.checkArgument(inverseSteps.size() == forwardSteps.size());
    this.forwardSteps = forwardSteps;
    this.inverseSteps = inverseSteps;
  }

  @Override
  public Codebase translate(
      Codebase toTranslate, Map<String, String> options, ProjectContext context)
      throws CodebaseCreationError {
    Preconditions.checkNotNull(
        options.get("referenceTargetCodebase"),
        "Inverse translation requires key 'referenceTargetCodebase'.");

    Deque<Codebase> forwardTranslationStack = makeForwardTranslationStack(options, context);

    Codebase referenceFromCodebase;
    // For the first reference from-codebase, use the 'referenceFromCodebase' option if given,
    // otherwise use the top of the forward-translation stack.
    if (options.get("referenceFromCodebase") != null) {
      try {
        Expression expression = Parser.parseExpression(options.get("referenceFromCodebase"));
        referenceFromCodebase = expressionEngine.createCodebase(expression, context);
      } catch (ParseError e) {
        throw new CodebaseCreationError(
            "Couldn't parse referenceFromCodebase '%s': %s",
            options.get("referenceFromCodebase"), e);
      }
      // Discard the "default" reference from-codebase, i.e. the top of the forward-trans stack.
      forwardTranslationStack.pop();
    } else {
      referenceFromCodebase = forwardTranslationStack.pop();
    }

    Codebase referenceTargetCodebase = forwardTranslationStack.peek();
    Codebase inverseTranslated = toTranslate;

    for (InverseTranslationStep inverseStep : inverseSteps) {
      try (Task task =
          ui.newTask(
              "inverseEdit",
              "Inverse-translating step %s by merging codebase %s onto %s",
              inverseStep.name(),
              referenceTargetCodebase,
              referenceFromCodebase)) {

        inverseTranslated =
            inverseStep
                .getInverseEditor()
                .inverseEdit(
                    inverseTranslated, referenceFromCodebase, referenceTargetCodebase, options);
        task.keep(inverseTranslated);
      }
      referenceFromCodebase = forwardTranslationStack.pop();
      referenceTargetCodebase = forwardTranslationStack.peek();
    }

    return inverseTranslated;
  }

  private Deque<Codebase> makeForwardTranslationStack(
      Map<String, String> options, ProjectContext context) throws CodebaseCreationError {
    Deque<Codebase> forwardTransStack = new ArrayDeque<>(forwardSteps.size() + 1);

    Codebase refTo;
    try (Task task =
        ui.newTask(
            "refTo",
            "Pushing to forward-translation stack: " + options.get("referenceTargetCodebase"))) {
      Expression expression = Parser.parseExpression(options.get("referenceTargetCodebase"));
      refTo = expressionEngine.createCodebase(expression, context);
      forwardTransStack.push(task.keep(refTo));
    } catch (ParseError e) {
      throw new CodebaseCreationError(e, "Couldn't parse in translation: %s", e);
    }

    // This Expression is used only for informative output.
    Expression forwardEditExp = refTo.expression();
    for (TranslationStep forwardStep : forwardSteps) {
      forwardEditExp = forwardEditExp.editWith(forwardStep.name, ImmutableMap.<String, String>of());
      try (Task task =
          ui.newTask("edit", "Pushing to forward-translation stack: " + forwardEditExp)) {
        refTo = forwardStep.editor.edit(refTo, options).copyWithExpression(forwardEditExp);
        forwardTransStack.push(task.keep(refTo));
      }
    }

    return forwardTransStack;
  }
}
