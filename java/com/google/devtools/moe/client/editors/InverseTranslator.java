// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.parser.Expression;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
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
 *
 */
public class InverseTranslator implements Translator {

  private final List<TranslatorStep> forwardSteps;
  private final List<InverseTranslatorStep> inverseSteps;

  public InverseTranslator(
      List<TranslatorStep> forwardSteps, List<InverseTranslatorStep> inverseSteps) {
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
        options.get("referenceToCodebase"),
        "Inverse translation requires key 'referenceToCodebase'.");

    Deque<Codebase> forwardTranslationStack = makeForwardTranslationStack(options, context);

    Codebase refFrom;
    // For the first reference from-codebase, use the 'referenceFromCodebase' option if given,
    // otherwise use the top of the forward-translation stack.
    if (options.get("referenceFromCodebase") != null) {
      try {
        refFrom =
            Parser.parseExpression(options.get("referenceFromCodebase")).createCodebase(context);
      } catch (ParseError e) {
        throw new CodebaseCreationError("Couldn't parse referenceFromCodebase '"
            + options.get("referenceFromCodebase") + "': " + e);
      }
      // Discard the "default" reference from-codebase, i.e. the top of the forward-trans stack.
      forwardTranslationStack.pop();
    } else {
      refFrom = forwardTranslationStack.pop();
    }

    Codebase refTo = forwardTranslationStack.peek();
    Codebase inverseTranslated = toTranslate;

    for (InverseTranslatorStep inverseStep : inverseSteps) {
      Task task =
          Injector.INSTANCE.ui().pushTask(
              "inverseEdit",
              String.format(
          "Inverse-translating step %s by merging codebase %s onto %s",
          inverseStep.getName(), refTo, refFrom));

      inverseTranslated = inverseStep.getInverseEditor().inverseEdit(
          inverseTranslated, refFrom, refTo, context, options);

      Injector.INSTANCE.ui().popTaskAndPersist(task, inverseTranslated.getPath());
      refFrom = forwardTranslationStack.pop();
      refTo = forwardTranslationStack.peek();
    }

    return inverseTranslated;
  }

  private Deque<Codebase> makeForwardTranslationStack(
      Map<String, String> options, ProjectContext context) throws CodebaseCreationError {
    Deque<Codebase> forwardTransStack = new ArrayDeque<Codebase>(forwardSteps.size() + 1);

    Codebase refTo;
    try {
      Task task =
          Injector.INSTANCE.ui().pushTask(
          "refTo", "Pushing to forward-translation stack: " + options.get("referenceToCodebase"));
      refTo = Parser.parseExpression(options.get("referenceToCodebase")).createCodebase(context);
      forwardTransStack.push(refTo);
      Injector.INSTANCE.ui().popTaskAndPersist(task, refTo.getPath());
    } catch (ParseError e) {
      throw new CodebaseCreationError("Couldn't parse in translation: " + e);
    }

    // This Expression is used only for informative output.
    Expression forwardEditExp = refTo.getExpression();
    for (TranslatorStep forwardStep : forwardSteps) {
      forwardEditExp = forwardEditExp.editWith(forwardStep.name, ImmutableMap.<String, String>of());
      Task task =
          Injector.INSTANCE.ui().pushTask(
              "edit", "Pushing to forward-translation stack: " + forwardEditExp);
      refTo = forwardStep.editor.edit(refTo, context, options).copyWithExpression(forwardEditExp);
      forwardTransStack.push(refTo);
      Injector.INSTANCE.ui().popTaskAndPersist(task, refTo.getPath());
    }

    return forwardTransStack;
  }
}
