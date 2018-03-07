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

package com.google.devtools.moe.client.codebase;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.FileCodebaseCreator;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;
import com.google.devtools.moe.client.writer.WritingError;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

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

  /**
   * Creates {@link Writer Writers} from {@link RepositoryExpression RepositoryExpressions}, using a
   * type-specific {@link WriterCreator} obtained from the {@link RepositoryType}.
   */
  @Singleton
  public static class WriterFactory {
    private final Ui ui;

    @Inject
    @VisibleForTesting
    public WriterFactory(Ui ui) {
      this.ui = ui;
    }

    /**
     * Create a {@link Writer} for this RepositoryExpression, e.g. new
     * RepositoryExpression("myGitRepo").atRevision("a983ef").makeWriter(context) checks out
     * Repository myGitRepo at commit id a983ef and returns a Writer encapsulating it.
     *
     * @throws WritingError
     */
    public Writer createWriter(RepositoryExpression expression, ProjectContext context)
        throws WritingError {
      RepositoryType repositoryType = context.getRepository(expression.term.identifier);
      WriterCreator wc = repositoryType.writerCreator();

      Ui.Task t = ui.pushTask("create_writer", "Creating Writer \"%s\"", expression.term);
      Writer writer = wc.create(expression.term.options);
      ui.popTaskAndPersist(t, writer.getRoot());
      return writer;
    }
  }

  @Override
  public String toString() {
    return term.toString();
  }

  /**
   * A {@link CodebaseProcessor} that creates an unmodified {@link Codebase} based on a {@link
   * RepositoryExpression}.
   */
  @Singleton
  public static class RepositoryCodebaseProcessor
      implements CodebaseProcessor<RepositoryExpression> {
    private final Ui ui;
    // TODO(cgruber) Replace this file-creator-or-context-get-creator patten with something cleaner.
    private final Provider<FileCodebaseCreator> fileCodebaseCreator;

    @Inject
    RepositoryCodebaseProcessor(Ui ui, Provider<FileCodebaseCreator> fileCodebaseCreator) {
      this.ui = ui;
      this.fileCodebaseCreator = fileCodebaseCreator;
    }

    @Override
    public Codebase createCodebase(RepositoryExpression expression, ProjectContext context)
        throws CodebaseCreationError {
      String repositoryName = expression.term.identifier;
      CodebaseCreator codebaseCreator =
          (repositoryName.equals("file"))
              ? fileCodebaseCreator.get() // for testing
              : context.getRepository(repositoryName).codebaseCreator();

      Ui.Task createTask = ui.pushTask("create_codebase", "Creating codebase for '%s'", this);
      try {
        Codebase codebase = codebaseCreator.create(expression.term.options);
        ui.popTaskAndPersist(createTask, codebase.path());
        return codebase;
      } catch (CodebaseCreationError e) {
        ui.message("%s", e);
        ui.popTask(createTask, "Unable to create codebase " + this);
        throw e;
      }
    }
  }
}
