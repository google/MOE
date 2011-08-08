// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.writer;

import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Term;

/**
 * A WriterExpression describes how to create a Writer.
 *
 * It is essentially the AST for a tiny DSL.
 *
 * Semantics:
 * An Expression has a Creator and Options.
 * The Creator is the name of a repository in the project.
 * The Options are a Map<String, String> which will be passed to the creator to create the codebase.
 * An Expression can be evaluated in a ProjectContext to return a Writer.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class WriterExpression {

  public final Term creator;

  public WriterExpression(Term creator) {
    this.creator = creator;
  }

  public String toString() {
    return creator.toString();
  }

  public static WriterExpression parse(String text) throws Parser.ParseError {
    return new WriterExpression(Parser.parseTermCompletely(text));
  }
}
