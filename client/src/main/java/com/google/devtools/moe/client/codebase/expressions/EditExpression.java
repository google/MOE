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

package com.google.devtools.moe.client.codebase.expressions;

import com.google.common.base.Preconditions;

/**
 * An expression encapsulating the transformation of the given Expression's Codebase via the
 * application of an {@link com.google.devtools.moe.client.translation.editors.Editor}. For example,
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

  public EditExpression(Expression exToEdit, Operation editOp) {
    Preconditions.checkArgument(Operator.EDIT.equals(editOp.operator()));
    this.exToEdit = exToEdit;
    this.editOp = editOp;
  }

  public Expression toEdit() {
    return exToEdit;
  }

  public Operation operation() {
    return editOp;
  }

  @Override
  public String toString() {
    return exToEdit.toString() + editOp;
  }

}
