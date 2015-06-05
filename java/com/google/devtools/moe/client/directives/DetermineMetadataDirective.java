// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.devtools.moe.client.MoeOptions;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.logic.DetermineMetadataLogic;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Parser.ParseError;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Combines the metadata for the given revisions into one consolidated metadata. Useful for when
 * multiple revisions in one repository need to be exported as one revision in the other.
 *
 */
public class DetermineMetadataDirective extends Directive {
  private final DetermineMetadataOptions options = new DetermineMetadataOptions();

  private final ProjectContextFactory contextFactory;
  private final Ui ui;

  @Inject
  DetermineMetadataDirective(ProjectContextFactory contextFactory, Ui ui) {
    this.contextFactory = contextFactory;
    this.ui = ui;
  }

  @Override
  public int perform() {
    ProjectContext context;
    try {
      context = contextFactory.create(options.configFilename);
    } catch (InvalidProject e) {
      ui.error(e, "Error creating project");
      return 1;
    }

    RepositoryExpression repoEx;
    try {
      repoEx = Parser.parseRepositoryExpression(options.repositoryExpression);
    } catch (ParseError e) {
      ui.error(e, "Couldn't parse " + options.repositoryExpression);
      return 1;
    }

    List<Revision> revs = Revision.fromRepositoryExpression(repoEx, context);

    RevisionMetadata rm = DetermineMetadataLogic.determine(context, revs, null);
    ui.info(rm.toString());
    return 0;
  }

  @Override
  public DetermineMetadataOptions getFlags() {
    return options;
  }

  @Override
  public String getDescription() {
    return "Consolidates the metadata for a set of revisions";
  }

  static class DetermineMetadataOptions extends MoeOptions {

    @Option(name = "--config_file", required = true, usage = "Location of MOE config file")
    String configFilename = "";

    @Option(
        name = "--revisions",
        required = true,
        usage = "Repository expression to get metadata for, e.g. 'internal(revision=3,4)'")
    String repositoryExpression = "";
  }
}
