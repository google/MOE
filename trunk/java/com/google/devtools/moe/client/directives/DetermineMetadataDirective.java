// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.logic.DetermineMetadataLogic;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionEvaluator;
import com.google.devtools.moe.client.repositories.RevisionExpression.RevisionExpressionError;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import org.kohsuke.args4j.Option;

import java.util.List;

/**
 * Combines the metadata for the given revisions into one
 * consolidated metadata. Useful for when multiple revisions
 * in one repository need to be exported as one revision in the other.
 *
 */
public class DetermineMetadataDirective implements Directive {

  private final DetermineMetadataOptions options = new DetermineMetadataOptions();

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = AppContext.RUN.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      AppContext.RUN.ui.error(e.explanation);
      return 1;
    }

    List<Revision> revs;
    try {
      revs = RevisionEvaluator.parseAndEvaluate(options.revisionExpression, context);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return 1;
    }
    if (revs.isEmpty()) {
      AppContext.RUN.ui.error(String.format(
          "No revisions found for expression: %s", options.revisionExpression));
      return 1;
    }

    RevisionMetadata rm = DetermineMetadataLogic.determine(context, revs, null);
    AppContext.RUN.ui.info(rm.toString());
    return 0;
  }

  @Override
  public DetermineMetadataOptions getFlags() {
    return options;
  }

  static class DetermineMetadataOptions extends MoeOptions {
    @Option(name = "--config_file", required = true,
            usage = "Location of MOE config file")
    String configFilename = "";
    @Option(name = "--revisions", required = true,
            usage = "Revision expression to determine metadata for")
    String revisionExpression = "";
  }
}
