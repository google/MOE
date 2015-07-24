// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;

import org.kohsuke.args4j.Option;

import javax.inject.Inject;

/**
 * Print the head revision of a repository.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class HighestRevisionDirective extends Directive {
  @Option(
    name = "--repository",
    required = true,
    usage =
        "Which repository expression to find the head revision for, e.g. 'internal' "
            + "or 'internal(revision=2)'"
  )
  String repository = "";

  private final Ui ui;

  @Inject
  HighestRevisionDirective(ProjectContextFactory contextFactory, Ui ui) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.ui = ui;
  }

  @Override
  protected int performDirectiveBehavior() {
    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(repository);
    } catch (ParseError e) {
      ui.error(e, "Couldn't parse " + repository);
      return 1;
    }

    Repository r = context().getRepository(repoEx.getRepositoryName());

    RevisionHistory rh = r.revisionHistory();
    if (rh == null) {
      ui.error("Repository " + r.name() + " does not support revision history.");
      return 1;
    }

    Revision rev = rh.findHighestRevision(repoEx.getOption("revision"));
    if (rev == null) {
      return 1;
    }

    ui.info("Highest revision in repository \"" + r.name() + "\": " + rev.revId());
    return 0;
  }

  @Override
  public String getDescription() {
    return "Finds the highest revision in a source control repository";
  }
}
