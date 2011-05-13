// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
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

  public HighestRevisionOptions getFlags() {
    return options;
  }

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
    Repository r = context.repositories.get(options.repository);
    if (r == null) {
      AppContext.RUN.ui.error("No repository " + options.repository);
      return 1;
    }

    RevisionHistory rh = r.revisionHistory;
    if (rh == null) {
      AppContext.RUN.ui.error("Repository " + r.name + " does not support revision history.");
      return 1;
    }
    Revision rev = rh.findHighestRevision(options.revision);
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
    @Option(name = "--revision",
            usage = "Highest revision to consider")
    String revision = "";
  }

}
