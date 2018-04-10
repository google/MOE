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

import com.google.auto.value.AutoValue;
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
@AutoValue
public abstract class EditExpression extends BinaryExpression {
  /** Package-only constructor visible by tests and the autovalue generated code. */
  EditExpression() {}

  public static EditExpression create(Expression operand, Operation operation) {
    Preconditions.checkArgument(Operator.EDIT.equals(operation.operator()));
    validateOperand(operand);
    return new AutoValue_EditExpression(operand, operation);
  }
}
