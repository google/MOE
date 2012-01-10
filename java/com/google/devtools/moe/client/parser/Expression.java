// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.parser;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.project.ProjectContext;

import java.util.Map;

/**
 * An interface for objects describing a {@link Codebase}. A Codebase is described lazily by
 * editing or translating a given expression. Then {@link Expression#createCodebase(ProjectContext)}
 * is called to create the Codebase in a given ProjectContext. Different Expression implementations
 * may offer different transformations, e.g.
 * {@link RepositoryExpression#createWriter(ProjectContext)}. Expressions should be immutable, and
 * all implementations of the transformations below should return new Expressions leaving the given
 * ones (this) unchanged.
 *
 */
public interface Expression {

  /**
   * Evaluate an Expression in the given context, creating the Codebase it describes. Do not assume
   * memoization: calling evaluate() twice may create the Codebase described by this expression
   * in two temp dirs.
   */
  Codebase createCodebase(ProjectContext context) throws CodebaseCreationError;

  /**
   * Transform this expression with the given translation {@link Operation}. For example, given an
   * Expression encapsulating "foo(revision=3)", calling expression.translateTo("public") yields an
   * expression encapsulating "foo(revision=3)>public".
   */
  TranslateExpression translateTo(String projectSpace);

  /**
   * A version of translateTo() used by the parser.
   */
  TranslateExpression translateTo(Operation translateOp);

  /**
   * Transform this expression with the given editing {@link Operation}. For example, given an
   * Expression encapsulating "foo(revision=3)", calling expression.editWith("editor",
   * { "option1": "foo", "option2": "bar" }) yields an expression encapsulating
   * "foo(revision=3)|editor(option1=foo,option2=bar)".
   */
  EditExpression editWith(String editorName, Map<String, String> editorOptions);

  /**
   * A version of editWith() used by the parser.
   */
  EditExpression editWith(Operation editOp);
}
