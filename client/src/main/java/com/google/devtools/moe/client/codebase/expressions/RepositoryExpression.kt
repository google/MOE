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
 * An [Expression] describing a repository checkout. This is the starting point for building
 * Expressions, e.g.: `RepositoryExpression("myGitRepo").atRevision("12345").translateTo("public").`
 */
data class RepositoryExpression(val term: Term) : Expression() {

  constructor(repositoryName: String) : this(Term(repositoryName))

  val repositoryName: String
    get() = term.identifier

  /** Add an option name-value pair to the expression, e.g. "myRepo" -> "myRepo(revision=4)". */
  fun withOption(optionName: String, optionValue: String): RepositoryExpression =
      RepositoryExpression(term.withOption(optionName, optionValue))

  /** Add multiple options to a repository.  */
  fun withOptions(options: Map<String, String>): RepositoryExpression =
      RepositoryExpression(term.withOptions(options))

  /** A helper method for adding a "revision" option with the given value. */
  fun atRevision(revId: String): RepositoryExpression = withOption("revision", revId)

  fun getOption(optionName: String): String? = term.options[optionName]

  override fun toString(): String = term.toString()
}
