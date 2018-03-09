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

package com.google.devtools.moe.client.codebase.expressions;

import com.google.common.collect.ImmutableMap;

/**
 * An {@link Expression} describing a repository checkout. This is the starting point for building
 * Expressions, e.g.:
 * new RepositoryExpression("myGitRepo").atRevision("a983ef").translateTo("public").
 */
public class RepositoryExpression extends AbstractExpression {

  private final Term term;

  /* As RepositoryExpression is the starting point for building Expressions, this should be the
   * only public constructor for any Expression class. */
  public RepositoryExpression(Term term) {
    this.term = term;
  }

  public RepositoryExpression(String repositoryName) {
    this(new Term(repositoryName, ImmutableMap.<String, String>of()));
  }

  public Term term() {
    return term;
  }

  /**
   * Add an option name-value pair to the expression, e.g. "myRepo" -> "myRepo(revision=4)".
   */
  public RepositoryExpression withOption(String optionName, String optionValue) {
    return new RepositoryExpression(term().withOption(optionName, optionValue));
  }

  /**
   * A helper method for adding a "revision" option with the given value.
   */
  public RepositoryExpression atRevision(String revId) {
    return withOption("revision", revId);
  }

  public String getRepositoryName() {
    return term().identifier;
  }

  public String getOption(String optionName) {
    return term().options.get(optionName);
  }

  @Override
  public String toString() {
    return term().toString();
  }

}
