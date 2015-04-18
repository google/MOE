// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.logic.OneMigrationLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
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
    String toProjectSpace;
    RepositoryExpression toRepoEx, fromRepoEx;
    try {
      context = Injector.INSTANCE.contextFactory.makeProjectContext(options.configFilename);
      toRepoEx = Parser.parseRepositoryExpression(options.toRepository);
      fromRepoEx = Parser.parseRepositoryExpression(options.fromRepository);
      toProjectSpace = context.config.getRepositoryConfig(toRepoEx.getRepositoryName())
          .getProjectSpace();
    } catch (ParseError e) {
      Injector.INSTANCE.ui.error(e, "Couldn't parse expression");
      return 1;
    } catch (InvalidProject e) {
      Injector.INSTANCE.ui.error(e, "Couldn't create project");
      return 1;
    }

    List<Revision> revs = Revision.fromRepositoryExpression(fromRepoEx, context);

    Codebase c;
    try {
      c = new RepositoryExpression(fromRepoEx.getRepositoryName())
          .atRevision(revs.get(0).revId)
          .translateTo(toProjectSpace)
          .createCodebase(context);
    } catch (CodebaseCreationError e) {
      Injector.INSTANCE.ui.error(e, "Error creating codebase");
      return 1;
    }

    Writer destination;
    try {
      destination = toRepoEx.createWriter(context);
    } catch (WritingError e) {
      Injector.INSTANCE.ui.error(e, "Error writing to repo");
      return 1;
    }

    Injector.INSTANCE.ui.info(String.format("Migrating '%s' to '%s'", fromRepoEx, toRepoEx));

    DraftRevision r = OneMigrationLogic.migrate(c, destination, revs, context, revs.get(0),
        fromRepoEx.getRepositoryName(), toRepoEx.getRepositoryName());
    if (r == null) {
      return 1;
    }

    Injector.INSTANCE.ui.info("Created Draft Revision: " + r.getLocation());
    return 0;
  }

  static class OneMigrationOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--from_repository", required = true,
            usage = "Repository expression to migrate from, e.g. 'internal(revision=3,4,5)'")
    String fromRepository = "";
    @Option(name = "--to_repository", required = true,
            usage = "Repository expression to migrate to, e.g. 'public(revision=7)'")
    String toRepository = "";
  }
}
