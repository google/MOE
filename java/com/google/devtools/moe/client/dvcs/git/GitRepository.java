// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.git;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.project.RepositoryType;
import com.google.devtools.moe.client.repositories.Repository;

import java.util.List;

/**
 * A helper class of static methods to create a Repository for Git.
 */
public class GitRepository {

  // Do not instantiate.
  private GitRepository() {}

  /**
   * Create a Repository from a RepositoryConfig indicating an Git repo ("type" == "git").
   *
   * @throws InvalidProject if RepositoryConfig is missing a repo URL.
   */
  public static Repository makeGitRepositoryFromConfig(
      final String name, final RepositoryConfig config)
      throws InvalidProject {
    Preconditions.checkArgument(config.getType() == RepositoryType.git);

    final String url = config.getUrl();
    if (url == null || url.isEmpty()) {
      throw new InvalidProject("Git repository config missing \"url\".");
    }

    Supplier<GitClonedRepository> freshSupplier = new Supplier<GitClonedRepository>() {
      @Override public GitClonedRepository get() {
        GitClonedRepository headClone = new GitClonedRepository(name, config);
        headClone.cloneLocallyAtHead();
        return headClone;
      }
    };
    
    Supplier<GitClonedRepository> memoizedSupplier = Suppliers.memoize(freshSupplier);
    
    GitRevisionHistory rh = new GitRevisionHistory(memoizedSupplier);

    String projectSpace = config.getProjectSpace();
    if (projectSpace == null) {
      projectSpace = "public";
    }

    GitCodebaseCreator cc = new GitCodebaseCreator(
        memoizedSupplier, rh, projectSpace, name, config);

    GitWriterCreator wc = new GitWriterCreator(freshSupplier, rh);

    return new Repository(name, rh, cc, wc);
  }
  
  /**
   * Run git with the specified args in the specified directory.
   */
  static String runGitCommand(List<String> args, String workingDirectory) 
      throws CommandRunner.CommandException {
    return AppContext.RUN.cmd.runCommand("git", args, workingDirectory);
  }
}
