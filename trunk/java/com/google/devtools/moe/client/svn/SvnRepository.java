// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.project.RepositoryType;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;

import java.util.List;

/**
 * Code to create a Repository for SVN.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnRepository {

  static public Repository makeSvnRepositoryFromConfig(
      String name, RepositoryConfig config) throws InvalidProject {
    Preconditions.checkArgument(config.getType() == RepositoryType.svn);

    String url = config.getUrl();
    if (url == null || url.isEmpty()) {
      throw new InvalidProject("Svn repository config missing \"url\".");
    }
    SvnRevisionHistory rh = new SvnRevisionHistory(name, url);

    String projectSpace = config.getProjectSpace();
    if (projectSpace == null) {
      projectSpace = "public";
    }

    SvnCodebaseCreator cc = new SvnCodebaseCreator(name, url, projectSpace, rh);

    SvnWriterCreator ec = new SvnWriterCreator(name, url, projectSpace, rh);

    return new Repository(name, rh, cc, ec);
  }

  static String runSvnCommand(List<String> args, String workingDirectory)
      throws CommandRunner.CommandException {
    ImmutableList.Builder<String> withAuthArgs = new ImmutableList.Builder<String>();
    withAuthArgs.add("--no-auth-cache").addAll(args);
    return AppContext.RUN.cmd.runCommand("svn", withAuthArgs.build(), "", workingDirectory);
  }
}
