package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.expressions.TranslateExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.translation.pipeline.TranslationPath;
import com.google.devtools.moe.client.translation.pipeline.TranslationPipeline;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A {@link CodebaseProcessor} that creates an translated {@link Codebase} based on a {@link
 * TranslateExpression}.
 */
@Singleton
public class TranslatedCodebaseProcessor implements CodebaseProcessor<TranslateExpression> {

  private final Ui ui;
  private final ExpressionEngine expressionEngine;

  @Inject
  public TranslatedCodebaseProcessor(Ui ui, ExpressionEngine expressionEngine) {
    this.ui = ui;
    this.expressionEngine = expressionEngine;
  }

  @Override
  public Codebase createCodebase(TranslateExpression expression, ProjectContext context)
      throws CodebaseCreationError {
    Codebase codebaseToTranslate =
        expressionEngine.createCodebase(expression.getOperand(), context);
    String toProjectSpace = expression.getOperation().getTerm().getIdentifier();
    TranslationPath path =
        TranslationPath.create(codebaseToTranslate.projectSpace(), toProjectSpace);
    TranslationPipeline translator = context.translators().get(path);
    if (translator == null) {
      throw new CodebaseCreationError(
          "Could not find translator from project space \"%s\" to \"%s\".\n"
              + "Translators only available for %s",
          codebaseToTranslate.projectSpace(), toProjectSpace, context.translators().keySet());
    }

    try (Task translateTask =
        ui.newTask(
            "translate",
            "Translating %s from project space \"%s\" to \"%s\"",
            codebaseToTranslate.root(),
            codebaseToTranslate.projectSpace(),
            toProjectSpace)) {

      Codebase translatedCodebase =
          translator.translate(
              codebaseToTranslate, expression.getOperation().getTerm().getOptions(), context);

      // Don't mark the translated codebase for persistence if it wasn't allocated by the
      // Translator.
      if (translatedCodebase.equals(codebaseToTranslate)) {
        translateTask.result().append(translatedCodebase.root() + " (unmodified)");
      } else {
        translateTask.keep(translatedCodebase);
      }
      return translatedCodebase.copyWithExpression(expression).copyWithProjectSpace(toProjectSpace);
    }
  }
}
