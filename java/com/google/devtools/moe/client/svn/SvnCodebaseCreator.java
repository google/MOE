// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.base.Predicate;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;

import java.io.File;
import java.util.Map;

/**
 * {@link CodebaseCreator} for svn.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnCodebaseCreator implements CodebaseCreator {

  private final String name;
  private final RepositoryConfig config;
  private final SvnRevisionHistory revisionHistory;
  private final SvnUtil util;

  public SvnCodebaseCreator(
      String repositoryName,
      RepositoryConfig config,
      SvnRevisionHistory revisionHistory,
      SvnUtil util) {
    this.name = repositoryName;
    this.config = config;
    this.revisionHistory = revisionHistory;
    this.util = util;
  }

  @Override
  public Codebase create(Map<String, String> options) throws CodebaseCreationError {
    String revId = options.get("revision");
    if (revId == null) {
      revId = "HEAD";
    }

    Revision rev = revisionHistory.findHighestRevision(revId);

    File exportPath =
        Injector.INSTANCE
            .fileSystem()
            .getTemporaryDirectory(String.format("svn_export_%s_%s_", name, rev.revId()));

    try {
      util.runSvnCommand(
          "export", config.getUrl(), "-r", rev.revId(), exportPath.getAbsolutePath());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem("could not export from svn" + e.getMessage());
    }

    // Filter codebase by ignore_file_res.
    final Predicate<CharSequence> nonIgnoredFilePred =
        Utils.nonMatchingPredicateFromRes(config.getIgnoreFileRes());
    Utils.filterFiles(exportPath, nonIgnoredFilePred);

    return new Codebase(
        exportPath, config.getProjectSpace(), new RepositoryExpression(new Term(name, options)));
  }
}
