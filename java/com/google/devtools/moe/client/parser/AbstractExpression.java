// Copyright 2011 The MOE Authors All Rights Reserved.

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
