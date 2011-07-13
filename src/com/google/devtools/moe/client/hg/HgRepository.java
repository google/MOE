// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import com.google.common.base.Preconditions;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.project.RepositoryType;
import com.google.devtools.moe.client.repositories.Repository;

import java.util.List;

/**
 * A helper class of static methods to create a Repository for Mercurial (herein Hg).
 *
 */
public class HgRepository {

  // Do not instantiate.
  private HgRepository() {}

  /**
   * Create a Repository from a RepositoryConfig indicating an Hg repo ("type" == "hg").
   *
   * @throws InvalidProject if RepositoryConfig is missing a repo URL.
   */
  public static Repository makeHgRepositoryFromConfig(String name, RepositoryConfig config)
      throws InvalidProject {
    Preconditions.checkArgument(config.getType() == RepositoryType.hg);

    String url = config.getUrl();
    if (url == null || url.isEmpty()) {
      throw new InvalidProject("Hg repository config missing \"url\".");
    }

    HgClonedRepository tipClone = new HgClonedRepository(name, url);
    tipClone.cloneLocally();

    HgRevisionHistory rh = new HgRevisionHistory(tipClone);

    String projectSpace = config.getProjectSpace();
    if (projectSpace == null) {
      projectSpace = "public";
    }

    HgCodebaseCreator cc = new HgCodebaseCreator(tipClone, rh, projectSpace);

    // TODO(user): Implement HgWriterCreator.
    return new Repository(name, rh, cc, null);
  }

  // TODO(user): Move this to an instance method on HgClonedRepository, so that all Hg command
  // calls implicitly occur in the context of a local cloned repo. Furthermore, via this command,
  // replace all absolute paths in command calls with paths relative to the local clone.
  static String runHgCommand(List<String> args, String workingDirectory)
      throws CommandRunner.CommandException {
    return AppContext.RUN.cmd.runCommand("hg", args, "", workingDirectory);
  }
}
