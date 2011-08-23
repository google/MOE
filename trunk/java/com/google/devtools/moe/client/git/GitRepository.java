// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

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
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitRepository {

  // Do not instantiate.
  private GitRepository() {}

  /**
   * Create a Repository from a RepositoryConfig indicating an Git repo ("type" == "git").
   *
   * @throws InvalidProject if RepositoryConfig is missing a repo URL.
   */
  public static Repository makeGitRepositoryFromConfig(final String name, RepositoryConfig config)
      throws InvalidProject {
    Preconditions.checkArgument(config.getType() == RepositoryType.git);

    final String url = config.getUrl();
    if (url == null || url.isEmpty()) {
      throw new InvalidProject("Git repository config missing \"url\".");
    }

    // Using a Supplier makes it possible to clone the repo lazily.
    // There are Directives such as CheckConfigDirective that need a Repository
    // but should not clone the repository as a side-effect. Therefore, this
    // makes it possible to create a Repository without forcing a clone.
    Supplier<GitClonedRepository> headCloneSupplier = Suppliers.memoize(
        new Supplier<GitClonedRepository>() {
          @Override
          public GitClonedRepository get() {
            GitClonedRepository headClone = new GitClonedRepository(name, url);
            headClone.cloneLocallyAtHead();
            return headClone;
          }
    });
    
    GitRevisionHistory rh = new GitRevisionHistory(headCloneSupplier);

    String projectSpace = config.getProjectSpace();
    if (projectSpace == null) {
      projectSpace = "public";
    }

    GitCodebaseCreator cc = new GitCodebaseCreator(headCloneSupplier, rh, projectSpace);

    GitWriterCreator wc = new GitWriterCreator(headCloneSupplier, rh, projectSpace);

    return new Repository(name, rh, cc, wc);
  }
  
  /**
   * Run git with the specified args in the specified directory.
   */
  static String runGitCommand(List<String> args, String workingDirectory) 
      throws CommandRunner.CommandException {
    return AppContext.RUN.cmd.runCommand("git", args, "", workingDirectory);
  }
}
