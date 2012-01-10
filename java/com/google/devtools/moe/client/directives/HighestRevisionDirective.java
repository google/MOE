// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;

import org.kohsuke.args4j.Option;

/**
 * Print the head revision of a repository.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class HighestRevisionDirective implements Directive {

  private final HighestRevisionOptions options = new HighestRevisionOptions();

  public HighestRevisionDirective() {}

  @Override
  public HighestRevisionOptions getFlags() {
    return options;
  }

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e, "Couldn't create project");
      return 1;
    }

    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(options.repository);
    } catch (ParseError e) {
      AppContext.RUN.ui.error(e, "Couldn't parse " + options.repository);
      return 1;
    }

    Repository r = context.repositories.get(repoEx.getRepositoryName());
    if (r == null) {
      AppContext.RUN.ui.error("No repository " + repoEx.getRepositoryName());
      return 1;
    }

    RevisionHistory rh = r.revisionHistory;
    if (rh == null) {
      AppContext.RUN.ui.error("Repository " + r.name + " does not support revision history.");
      return 1;
    }

    Revision rev = rh.findHighestRevision(repoEx.getOption("revision"));
    if (rev == null) {
      return 1;
    }

    AppContext.RUN.ui.info("Highest revision in repository \"" + r.name + "\": " + rev.revId);
    return 0;
  }

  static class HighestRevisionOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--repository", required = true,
            usage = "Which repository expression to find the head revision for, e.g. 'internal' " +
                    "or 'internal(revision=2)'")
    String repository = "";
  }
}
