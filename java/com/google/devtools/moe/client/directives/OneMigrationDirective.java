// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.codebase.Evaluator;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.logic.OneMigrationLogic;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionEvaluator;
import com.google.devtools.moe.client.repositories.RevisionExpression;
import com.google.devtools.moe.client.repositories.RevisionExpression.RevisionExpressionError;
import com.google.devtools.moe.client.testing.DummyDb;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterEvaluator;
import com.google.devtools.moe.client.writer.WriterExpression;
import com.google.devtools.moe.client.writer.WritingError;

import org.kohsuke.args4j.Option;

import java.util.List;

/**
 * Perform a single migration using command line flags.
 *
 */
public class OneMigrationDirective implements Directive {

  private final OneMigrationOptions options = new OneMigrationOptions();

  public OneMigrationDirective() {}

  @Override
  public OneMigrationOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e.explanation);
      return 1;
    }

    Db db;
    if (options.dbLocation.equals("dummy")) {
      db = new DummyDb(true);
    } else {
      // TODO(user): also allow for url dbLocation types
      try {
        db = new FileDb(FileDb.makeDbFromFile(options.dbLocation));
      } catch (MoeProblem e) {
        AppContext.RUN.ui.error(e.explanation);
        return 1;
      }
    }

    RevisionExpression fromRe;
    try {
      fromRe = RevisionExpression.parse(options.fromRevision);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }

    RevisionExpression toRe;
    try {
      toRe = RevisionExpression.parse(options.toRevision);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }

    Repository toRepo = context.repositories.get(toRe.repoId);
    if (toRepo == null) {
      AppContext.RUN.ui.error("No repository " + toRe.repoId);
      return 1;
    }
    String toProjectSpace = toRepo.codebaseCreator.getProjectSpace();
    Codebase c;
    try {
      CodebaseExpression ce = fromRe.toCodebaseExpression(toProjectSpace, context);
      if (ce == null) {
        return 1;
      }
      c = Evaluator.evaluate(ce, context);
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }

    Writer destination;
    try {
      WriterExpression we = toRe.toWriterExpression(context);
      if (we == null) {
        return 1;
      }
      destination = WriterEvaluator.evaluate(we, context);
    } catch (WritingError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }

    AppContext.RUN.ui.info(
        String.format("Creating a change in \"%s\" with revisions \"%s\"",
                      toRe.repoId, options.revisionsToMigrate));

    List<Revision> revs;
    try {
      revs = RevisionEvaluator.parseAndEvaluate(options.revisionsToMigrate, context);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }
    if (revs.isEmpty()) {
      AppContext.RUN.ui.error("No revision ids specified to migrate in Revision Expression: " +
        options.revisionsToMigrate);
      return 1;
    }
    DraftRevision r = OneMigrationLogic.migrate(db, c, destination, revs, context);
    if (r == null) {
      return 1;
    }

    AppContext.RUN.ui.info("Created Draft Revision: " + r.getLocation());
    return 0;
  }

  static class OneMigrationOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--db", required = true,
            usage = "Location of MOE database")
    String dbLocation = "";
    @Option(name = "--from_revision", required = true,
            usage = "Revision expression of source")
    String fromRevision = "";
    @Option(name = "--to_revision", required = true,
            usage = "Revsion expression of destination")
    String toRevision = "";
    @Option(name = "--revisions_to_migrate", required = true,
            usage = "Revision expression")
    String revisionsToMigrate = "";
  }
}
