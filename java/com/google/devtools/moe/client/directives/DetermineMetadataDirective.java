// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.logic.DetermineMetadataLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
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
      context = Injector.INSTANCE.contextFactory.makeProjectContext(options.configFilename);
    } catch (InvalidProject e) {
      Injector.INSTANCE.ui.error(e, "Error creating project");
      return 1;
    }

    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(options.repositoryExpression);
    } catch (ParseError e) {
      Injector.INSTANCE.ui.error(
          e, "Couldn't parse " + options.repositoryExpression);
      return 1;
    }

    List<Revision> revs = Revision.fromRepositoryExpression(repoEx, context);

    RevisionMetadata rm = DetermineMetadataLogic.determine(context, revs, null);
    Injector.INSTANCE.ui.info(rm.toString());
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
            usage = "Repository expression to get metadata for, e.g. 'internal(revision=3,4)'")
    String repositoryExpression = "";
  }
}
