// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.parser.Operation;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Term;

import java.io.StreamTokenizer;
import java.util.Collections;
import java.util.List;

/**
 * A CodebaseExpression describes how to create a Codebase.
 *
 * It is essentially the AST for a tiny DSL.
 *
 * A CodebaseExpression has exactly one creator Term. It may then have arbitrarily many
 * editor Terms, each preceded by a |.
 *
 * Semantics:
 * An Expression has a Creator and Options.
 * The Creator is the name of a repository in the project.
 * The Options are a Map<String, String> which will be passed to the creator to create the codebase.
 * An Expression can be evaluated in a ProjectContext to return a Codebase.
 *
 * Extensions:
 * There will be creators unassociated with repositories. E.g., the file creator will take a
 * directory or tarball already created.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CodebaseExpression {

  public final Term creator;
  public final List<Operation> operations;

  public CodebaseExpression(Term creator) {
    this.creator = creator;
    this.operations = ImmutableList.of();
  }

  public CodebaseExpression(Term creator, List<Operation> operations) {
    this.creator = creator;
    this.operations = Collections.unmodifiableList(operations);
  }

  public String toString() {
    StringBuilder r = new StringBuilder();
    r.append(creator.toString());
    for (Operation o: operations) {
      r.append(o.operator);
      r.append(o.term.toString());
    }
    return r.toString();
  }

  public static CodebaseExpression parse(String text) throws Parser.ParseError {
    StreamTokenizer t = Parser.tokenize(text);
    Term creator = Parser.parseTerm(t);

    List<Operation> operations = Parser.parseOperationList(t);
    return new CodebaseExpression(creator, operations);
  }

  public boolean equals(Object o) {
    if (!(o instanceof CodebaseExpression)) {
      return false;
    }
    return toString().equals(o.toString());
  }
}
