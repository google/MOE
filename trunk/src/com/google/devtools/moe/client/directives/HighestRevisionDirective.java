// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionExpression;
import com.google.devtools.moe.client.repositories.RevisionExpression.RevisionExpressionError;
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
    if (options.configFilename.isEmpty()) {
      System.err.println("No --config_file specified.");
      return 1;
    }
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      System.err.println(e.explanation);
      return 1;
    }

    if (options.repository.isEmpty()) {
      AppContext.RUN.ui.error("No --repository specified");
      return 1;
    }
    
    RevisionExpression re;
    try {
      re = RevisionExpression.parse(options.repository);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }
    
    Repository r = context.repositories.get(re.repoId);
    if (r == null) {
      AppContext.RUN.ui.error("No repository " + options.repository);
      return 1;
    }

    RevisionHistory rh = r.revisionHistory;
    if (rh == null) {
      AppContext.RUN.ui.error("Repository " + r.name + " does not support revision history.");
      return 1;
    }
    
    Revision rev;
    if (re.revIds.isEmpty()) {
      rev = rh.findHighestRevision("");
    } else if (re.revIds.size() == 1) {
      rev = rh.findHighestRevision(re.revIds.get(0));
    } else {
      AppContext.RUN.ui.error("Only one revision can be specified for this directive.");
      return 1;
    }
    AppContext.RUN.ui.info("Highest revision in repository \"" + r.name + "\": " + rev.revId);
    return 0;
  }

  static class HighestRevisionOptions extends MoeOptions {
    @Option(name = "--config_file",
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--repository",
            usage = "Which repository to find the head revision for")
    String repository = "";
  }

}
