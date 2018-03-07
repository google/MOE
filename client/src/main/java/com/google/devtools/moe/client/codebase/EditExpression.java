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

package com.google.devtools.moe.client.codebase;

import com.google.common.base.Preconditions;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.translation.editors.Editor;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * An expression encapsulating the transformation of the given Expression's Codebase via the
 * application of an {@link Editor}. For example,
 *
 * <pre>{@code
 * new RepositoryExpression("myRepo").editWith("myEditor", { "option1": "foo" })
 * }</pre>
 *
 * returns an EditExpression for
 *
 * <pre>{@code "myRepo|myEditor(option1=foo)"}</pre>
 *
 * .
 */
public class EditExpression extends AbstractExpression {

  private final Expression exToEdit;
  private final Operation editOp;

  EditExpression(Expression exToEdit, Operation editOp) {
    Preconditions.checkArgument(editOp.operator == Operator.EDIT);
    this.exToEdit = exToEdit;
    this.editOp = editOp;
  }

  @Override
  public String toString() {
    return exToEdit.toString() + editOp;
  }

  @Singleton
  public static class EditedCodebaseProcessor implements CodebaseProcessor<EditExpression> {
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
      Codebase codebaseToEdit = expressionEngine.createCodebase(expression.exToEdit, context);
      String editorName = expression.editOp.term.identifier;
      Editor editor = context.editors().get(editorName);
      if (editor == null) {
        throw new CodebaseCreationError("no editor %s", editorName);
      }

      Ui.Task editTask =
          ui.pushTask(
              "edit", "Editing %s with editor %s", codebaseToEdit.path(), editor.getDescription());

      Codebase editedCodebase = editor.edit(codebaseToEdit, expression.editOp.term.options);

      ui.popTaskAndPersist(editTask, editedCodebase.path());
      return editedCodebase.copyWithExpression(expression);
    }
  }
}
