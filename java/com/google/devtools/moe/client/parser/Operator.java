// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.parser;

/**
 * Operators in the MOE Codebase Expression Language.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public enum Operator {
  EDIT('|'),
  TRANSLATE('>');

  private final char op;

  Operator(char op) {
    this.op = op;
  }

  @Override
  public String toString() {
    return String.valueOf(op);
  }

  public static Operator getOperator(char c) throws IllegalArgumentException {
    if (c == '|') {
      return EDIT;
    }
    if (c == '>') {
      return TRANSLATE;
    }
    throw new IllegalArgumentException("Invalid operator: " + c);
  }
}
