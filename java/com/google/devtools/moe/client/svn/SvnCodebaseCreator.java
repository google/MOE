// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.repositories.Revision;

import java.io.File;
import java.util.Map;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnCodebaseCreator implements CodebaseCreator {

  private final String name;
  private final String url;
  private final String projectSpace;
  private final SvnRevisionHistory revisionHistory;

  public SvnCodebaseCreator(String repositoryName, String url, String projectSpace,
                            SvnRevisionHistory revisionHistory) {
    this.name = repositoryName;
    this.url = url;
    this.projectSpace = projectSpace;
    this.revisionHistory = revisionHistory;
  }

  public Codebase create(Map<String, String> options) throws CodebaseCreationError{
    String revId = options.get("revision");
    if (revId == null) {
      revId = "HEAD";
    }

    Revision rev = revisionHistory.findHighestRevision(revId);

    File exportPath = AppContext.RUN.fileSystem.getTemporaryDirectory(
        String.format("svn_export_%s_%s_", name, rev.revId));

    try {
      SvnRepository.runSvnCommand(
          ImmutableList.of("export", url, "-r", rev.revId, exportPath.getAbsolutePath()), "");
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem("could not export from svn" + e.getMessage());
    }
    return new Codebase(
        exportPath, projectSpace,
        new CodebaseExpression(new Term(name, options)));
  }

  public String getProjectSpace() {
    return projectSpace;
  }
}
