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

package com.google.devtools.moe.client.parser;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.FileCodebaseCreator;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;
import com.google.devtools.moe.client.writer.WritingError;

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

  /**
   * Add an option name-value pair to the expression, e.g. "myRepo" -> "myRepo(revision=4)".
   */
  public RepositoryExpression withOption(String optionName, String optionValue) {
    return new RepositoryExpression(term.withOption(optionName, optionValue));
  }

  /**
   * A helper method for adding a "revision" option with the given value.
   */
  public RepositoryExpression atRevision(String revId) {
    return withOption("revision", revId);
  }

  public String getRepositoryName() {
    return term.identifier;
  }

  public String getOption(String optionName) {
    return term.options.get(optionName);
  }

  @Override
  public Codebase createCodebase(ProjectContext context) throws CodebaseCreationError {
    String repositoryName = term.identifier;
    CodebaseCreator cc;
    if (repositoryName.equals("file")) {
      cc = new FileCodebaseCreator(Injector.INSTANCE.fileSystem());
    } else {
      RepositoryType repo = context.getRepository(repositoryName);
      cc = repo.codebaseCreator();
    }

    Ui.Task createTask =
        Injector.INSTANCE.ui().pushTask("create_codebase", "Creating codebase for '%s'", this);
    Codebase c = cc.create(term.options);
    Injector.INSTANCE.ui().popTaskAndPersist(createTask, c.getPath());
    return c;
  }

  /**
   * Create a {@link Writer} for this RepositoryExpression, e.g.
   * new RepositoryExpression("myGitRepo").atRevision("a983ef").makeWriter(context)
   * checks out Repository myGitRepo at commit id a983ef and returns a Writer encapsulating it.
   *
   * @throws WritingError
   */
  public Writer createWriter(ProjectContext context) throws WritingError {
    RepositoryType r = context.getRepository(term.identifier);
    WriterCreator wc = r.writerCreator();

    Ui.Task t = Injector.INSTANCE.ui().pushTask("create_writer", "Creating Writer \"%s\"", term);
    Writer writer = wc.create(term.options);
    Injector.INSTANCE.ui().popTaskAndPersist(t, writer.getRoot());
    return writer;
  }

  @Override
  public String toString() {
    return term.toString();
  }
}
