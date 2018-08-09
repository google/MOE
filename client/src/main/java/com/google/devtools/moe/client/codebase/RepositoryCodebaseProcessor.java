package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
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
    String repositoryName = expression.getTerm().getIdentifier();
    CodebaseCreator codebaseCreator =
        (repositoryName.equals("file"))
            ? fileCodebaseCreator.get() // for testing
            : context.getRepository(repositoryName).codebaseCreator();

    try (Task createTask =
        ui.newTask("create_codebase", "Creating codebase for '%s'", expression)) {
      try {
        return createTask.keep(codebaseCreator.create(expression.getTerm().getOptions()));
      } catch (CodebaseCreationError e) {
        ui.message("%s", e);
        createTask.result().append("Unable to create codebase " + this);
        throw e;
      }
    }
  }
}
