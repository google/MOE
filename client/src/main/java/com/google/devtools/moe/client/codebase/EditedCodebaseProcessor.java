package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.expressions.EditExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.translation.editors.Editor;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Peforms the codebase transformation for an EditExpression. */
@Singleton
public class EditedCodebaseProcessor implements CodebaseProcessor<EditExpression> {

  private final Ui ui;
  private final ExpressionEngine expressionEngine;

  @Inject
  EditedCodebaseProcessor(Ui ui, ExpressionEngine expressionEngine) {
    this.ui = ui;
    this.expressionEngine = expressionEngine;
  }

  @Override
  public Codebase createCodebase(EditExpression expression, ProjectContext context)
      throws CodebaseCreationError {
    Codebase codebaseToEdit = expressionEngine.createCodebase(expression.getOperand(), context);
    String editorName = expression.getOperation().getTerm().getIdentifier();
    Editor editor = context.editors().get(editorName);
    if (editor == null) {
      throw new CodebaseCreationError("no editor %s", editorName);
    }

    try (Task task =
        ui.newTask(
            "edit", "Editing %s with editor %s", codebaseToEdit.root(), editor.getDescription())) {
      return task.keep(
              editor.edit(codebaseToEdit, expression.getOperation().getTerm().getOptions()))
          .copyWithExpression(expression);
    }
  }
}
