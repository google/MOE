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

package com.google.devtools.moe.client.parser;

import com.google.common.base.Preconditions;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Task;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.project.ProjectContext;

/**
 * An expression encapsulating the transformation of the given Expression's Codebase via the
 * application of an {@link Editor}. For example,
 * new RepositoryExpression("myRepo").editWith("myEditor", { "option1": "foo" })
 * returns an EditExpression for "myRepo|myEditor(option1=foo)".
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
  public Codebase createCodebase(ProjectContext context) throws CodebaseCreationError {
    Codebase codebaseToEdit = exToEdit.createCodebase(context);
    String editorName = editOp.term.identifier;
    Editor editor = context.editors().get(editorName);
    if (editor == null) {
      throw new CodebaseCreationError("no editor " + editorName);
    }

    Task editTask =
        Injector.INSTANCE
            .getUi()
            .pushTask(
                "edit",
                "Editing %s with editor %s",
                codebaseToEdit.getPath(),
                editor.getDescription());

    Codebase editedCodebase = editor.edit(codebaseToEdit, context, editOp.term.options);

    Injector.INSTANCE.getUi().popTaskAndPersist(editTask, editedCodebase.getPath());
    return editedCodebase.copyWithExpression(this);
  }

  @Override
  public String toString() {
    return "" + exToEdit + editOp;
  }
}
