// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.parser;

/**
 * An Operation in the MOE Expression Language is an operator followed by a term.
 *
 * E.g., |patch(file="/path/to/path.txt") or >public
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Operation {

  public final Operator operator;
  public final Term term;

  public Operation(Operator operator, Term term) {
    this.operator = operator;
    this.term = term;
  }

  public String toString() {
    return operator.toString() + term.toString();
  }

  public boolean equals(Object o) {
    if (!(o instanceof Operation)) {
      return false;
    }
    return toString().equals(o.toString());
  }

}
