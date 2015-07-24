// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.logic.OneMigrationLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Perform a single migration using command line flags.
 *
 */
public class OneMigrationDirective extends Directive {
  @Option(
    name = "--from_repository",
    required = true,
    usage = "Repository expression to migrate from, e.g. 'internal(revision=3,4,5)'"
  )
  String fromRepository = "";

  @Option(
    name = "--to_repository",
    required = true,
    usage = "Repository expression to migrate to, e.g. 'public(revision=7)'"
  )
  String toRepository = "";

  private final Ui ui;

  @Inject
  OneMigrationDirective(ProjectContextFactory contextFactory, Ui ui) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    String toProjectSpace;
    RepositoryExpression toRepoEx, fromRepoEx;
    try {
      toRepoEx = Parser.parseRepositoryExpression(toRepository);
      fromRepoEx = Parser.parseRepositoryExpression(fromRepository);
      toProjectSpace =
          context().config.getRepositoryConfig(toRepoEx.getRepositoryName()).getProjectSpace();
    } catch (ParseError e) {
      ui.error(e, "Couldn't parse expression");
      return 1;
    }

    List<Revision> revs = Revision.fromRepositoryExpression(fromRepoEx, context());

    Codebase c;
    try {
      c =
          new RepositoryExpression(fromRepoEx.getRepositoryName())
              .atRevision(revs.get(0).revId())
              .translateTo(toProjectSpace)
              .createCodebase(context());
    } catch (CodebaseCreationError e) {
      ui.error(e, "Error creating codebase");
      return 1;
    }

    Writer destination;
    try {
      destination = toRepoEx.createWriter(context());
    } catch (WritingError e) {
      ui.error(e, "Error writing to repo");
      return 1;
    }

    ui.info("Migrating '%s' to '%s'", fromRepoEx, toRepoEx);

    DraftRevision r =
        OneMigrationLogic.migrate(
            c,
            destination,
            revs,
            context(),
            revs.get(0),
            fromRepoEx.getRepositoryName(),
            toRepoEx.getRepositoryName());
    if (r == null) {
      return 1;
    }

    ui.info("Created Draft Revision: " + r.getLocation());
    return 0;
  }

  @Override
  public String getDescription() {
    return "Performs a single migration";
  }
}
