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

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * A skeletal implementation of the {@link Expression} interface.
 *
 */
// TODO(cgruber) @Autovalue or at least fix hashcode and equals.
public abstract class AbstractExpression implements Expression {

  @Override
  public EditExpression editWith(Operation editOp) {
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
  public TranslateExpression translateTo(Operation translateOp) {
    return new TranslateExpression(this, translateOp);
  }

  @Override
  public abstract String toString();

  @Override
  public boolean equals(Object other) {
    return this.getClass().equals(other.getClass()) && this.toString().equals(other.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
