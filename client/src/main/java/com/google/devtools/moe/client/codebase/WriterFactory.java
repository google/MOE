package com.google.devtools.moe.client.codebase;

import com.google.common.annotations.VisibleForTesting;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;
import com.google.devtools.moe.client.writer.WritingError;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Creates {@link Writer Writers} from {@link RepositoryExpression RepositoryExpressions}, using a
 * type-specific {@link WriterCreator} obtained from the {@link RepositoryType}.
 */
@Singleton
public class WriterFactory {
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
    RepositoryType repositoryType = context.getRepository(expression.getTerm().getIdentifier());
    WriterCreator wc = repositoryType.writerCreator();

    try (Task task = ui.newTask("create_writer", "Creating Writer \"%s\"", expression.getTerm())) {
      return task.keep(wc.create(expression.getTerm().getOptions()));
    }
  }
}
