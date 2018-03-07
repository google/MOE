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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;

/**
 * A skeletal implementation of the {@link Expression} interface.
 *
 * <pre>{@code
 * Minimalism
 * Abstract Expressionism
 * Postmodernism
 * Is it?"
 *     - Living Color, "Type" from the album "Time's Up"
 * }</pre>
 */
// TODO(cgruber) @Autovalue or at least fix hashcode and equals.
public abstract class AbstractExpression extends Expression {

  @Override
  EditExpression editWith(Operation editOp) {
    return new EditExpression(this, editOp);
  }

  @Override
  public EditExpression editWith(String editorName, Map<String, String> editorOptions) {
    Term term = new Term(editorName, editorOptions);
    Operation op = new Operation(Operator.EDIT, term);
    return editWith(op);
  }

  @Override
  public TranslateExpression translateTo(String projectSpace) {
    Term term = new Term(projectSpace, ImmutableMap.<String, String>of());
    Operation op = new Operation(Operator.TRANSLATE, term);
    return translateTo(op);
  }

  @Override
  TranslateExpression translateTo(Operation translateOp) {
    return new TranslateExpression(this, translateOp);
  }

  @Override
  public abstract String toString();

  @Override
  public boolean equals(Object other) {
    return other != null
        && Objects.equals(this.getClass(), other.getClass())
        && Objects.equals(this.toString(), other.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
