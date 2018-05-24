package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.testing.FileCodebaseCreator;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * A {@link CodebaseProcessor} that creates an unmodified {@link Codebase} based on a {@link
 * RepositoryExpression}.
 */
@Singleton
public class RepositoryCodebaseProcessor implements CodebaseProcessor<RepositoryExpression> {
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
    String repositoryName = expression.term().identifier();
    CodebaseCreator codebaseCreator =
        (repositoryName.equals("file"))
            ? fileCodebaseCreator.get() // for testing
            : context.getRepository(repositoryName).codebaseCreator();

    Ui.Task createTask = ui.pushTask("create_codebase", "Creating codebase for '%s'", expression);
    try {
      Codebase codebase = codebaseCreator.create(expression.term().options());
      ui.popTaskAndPersist(createTask, codebase.path());
      return codebase;
    } catch (CodebaseCreationError e) {
      ui.message("%s", e);
      ui.popTask(createTask, "Unable to create codebase " + this);
      throw e;
    }
  }
}
