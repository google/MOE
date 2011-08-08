// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.codebase.Evaluator;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionEvaluator;
import com.google.devtools.moe.client.repositories.RevisionExpression;
import com.google.devtools.moe.client.repositories.RevisionExpression.RevisionExpressionError;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterEvaluator;
import com.google.devtools.moe.client.writer.WriterExpression;
import com.google.devtools.moe.client.writer.WritingError;

import java.util.List;

/**
 * Perform the one_migration and migrate directives
 *
 */
public class OneMigrationLogic {

  /**
   * Perform a migration from specified source, destination, and specified Revisions
   *
   * @param db  the MOE Db storing equivalences
   * @param c the Codebase to use as source
   * @param destination the Writer to put the files from c into
   * @param revisionsToMigrate  all revisions to include in this migration (the metadata from the
   *                            Revisions in revisionsToMigrate determines the metadata for the
   *                            change)
   *
   * @return  a DraftRevision on success, or null on failure
   */
  public static DraftRevision migrate(Db db, Codebase c, Writer destination,
                                      List<Revision> revisionsToMigrate) {
    //TODO(user): call determineMetadata on revisionsToMigrate, pass result to change after
    //scrubbing; allow use of migration mode
    DraftRevision r = ChangeLogic.change(c, destination);
    if (r == null) {
      return null;
    } else {
      //TODO(user): add new equivalence to db
      return r;
    }
  }

  /**
   * Perform a migration from a Migration object
   *
   * @param db  the MOE Db storing equivalences
   * @param migration the Migration representing the migration to perform
   * @param context the context to evaluate in
   *
   * @return  a DraftRevision on success, or null on failure
   */
  public static DraftRevision migrate(Db db, Migration migration,
                                      ProjectContext context) {
    Repository toRepo = context.repositories.get(migration.toRepository);
    String toProjectSpace = toRepo.codebaseCreator.getProjectSpace();
    Codebase c;
    RevisionExpression fromRe = new RevisionExpression(migration.fromRepository,
                                                       ImmutableList.<String>of());
    try {
      CodebaseExpression ce = fromRe.toCodebaseExpression(toProjectSpace, context);
      if (ce == null) {
        return null;
      }
      c = Evaluator.evaluate(ce, context);
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return null;
    }

    RevisionExpression toRe = new RevisionExpression(migration.toRepository,
                                                     ImmutableList.<String>of());
    WriterExpression we = toRe.toWriterExpression(context);
    if (we == null) {
      return null;
    }
    Writer destination;
    try {
      destination = WriterEvaluator.evaluate(we, context);
    } catch (WritingError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return null;
    }

    List<Revision> revisions;
    try {
      revisions = RevisionEvaluator.getRevisionsFromRe(db, context, fromRe,
                                                       migration.toRepository);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return null;
    }
    return migrate(db, c, destination, revisions);
  }
}
