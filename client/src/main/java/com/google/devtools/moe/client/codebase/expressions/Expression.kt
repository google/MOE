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

package com.google.devtools.moe.client.codebase.expressions

/**
 * An API for objects describing a [com.google.devtools.moe.client.codebase.Codebase]. A
 * `Codebase` is described lazily by editing or translating a given expression. Then
 * [com.google.devtools.moe.client.codebase.ExpressionEngine.createCodebase]
 * is called to create the materialized `Codebase` from the [Expression], in a given
 * [com.google.devtools.moe.client.project.ProjectContext]. Different Expression types will
 * have an associated [com.google.devtools.moe.client.codebase.CodebaseProcessor] which may
 * offer different alterations from the base [RepositoryExpression]. Expressions should be
 * immutable, and all implementations of the transformations should return new Expressions.
 */
abstract class Expression {
  /**
   * Transform this expression with the given translation [Operation]. For example, given an
   * Expression encapsulating "foo(revision=3)", calling expression.translateTo("public") yields an
   * expression encapsulating "foo(revision=3)>public".
   */
  fun translateTo(projectSpace: String): TranslateExpression {
    val term = Term(projectSpace)
    val op = Operation(Operator.TRANSLATE, term)
    return translateTo(op)
  }

  /** A version of translateTo() used by the parser.  */
  protected fun translateTo(translateOp: Operation): TranslateExpression =
      TranslateExpression(this, translateOp)

  /**
   * Transform this expression with the given editing [Operation]. For example, given an
   * Expression encapsulating "foo(revision=3)", calling expression.editWith("editor", { "option1":
   * "foo", "option2": "bar" }) yields an expression encapsulating
   * "foo(revision=3)|editor(option1=foo,option2=bar)".
   */
  fun editWith(editorName: String, editorOptions: Map<String, String>): EditExpression {
    val term = Term(editorName).withOptions(editorOptions)
    val op = Operation(Operator.EDIT, term)
    return editWith(op)
  }

  /** A version of editWith() used by the parser.  */
  protected fun editWith(editOp: Operation): EditExpression = EditExpression(this, editOp)
}
