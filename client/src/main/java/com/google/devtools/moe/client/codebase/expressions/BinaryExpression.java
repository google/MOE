package com.google.devtools.moe.client.codebase.expressions;

import static com.google.common.base.Preconditions.checkState;

import java.util.HashSet;
import java.util.Set;

/** Bundles the API in common for operator/operand style expressions. */
abstract class BinaryExpression extends AbstractExpression {

  public abstract Expression operand();

  public abstract Operation operation();

  @Override
  public String toString() {
    return "" + operand() + operation();
  }

  protected static void validateOperand(Expression e) {
    validateOperand(new HashSet<>(), e);
  }

  protected static void validateOperand(Set<Expression> visited, Expression e) {
    if (e instanceof BinaryExpression) {
      validateOperand(visited, (BinaryExpression) e);
    }
  }

  protected static void validateOperand(Set<Expression> visited, BinaryExpression e) {
    checkState(
        !visited.contains(e.operand()),
        "Cyclic inclusion in Expressions. Start from a RepositoryExpression.");
    visited.add(e.operand());
    validateOperand(visited, e.operand());
  }
}
