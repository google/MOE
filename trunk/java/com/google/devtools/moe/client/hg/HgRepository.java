// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

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
  public static Repository makeHgRepositoryFromConfig(final String name,
      RepositoryConfig config) throws InvalidProject {
    Preconditions.checkArgument(config.getType() == RepositoryType.hg);

    final String url = config.getUrl();
    if (url == null || url.isEmpty()) {
      throw new InvalidProject("Hg repository config missing \"url\".");
    }

    // Using a Supplier makes it possible to clone the repo lazily.
    // There are Directives such as CheckConfigDirective that need a Repository
    // but should not clone the repository as a side-effect. Therefore, this
    // makes it possible to create a Repository without forcing a clone.
    Supplier<HgClonedRepository> tipCloneSupplier = Suppliers.memoize(
        new Supplier<HgClonedRepository>() {
          @Override
          public HgClonedRepository get() {
            HgClonedRepository tipClone = new HgClonedRepository(name, url);
            tipClone.cloneLocallyAtTip();
            return tipClone;
          }
    });

    HgRevisionHistory rh = new HgRevisionHistory(tipCloneSupplier);

    String projectSpace = config.getProjectSpace();
    if (projectSpace == null) {
      projectSpace = "public";
    }

    HgCodebaseCreator cc = new HgCodebaseCreator(tipCloneSupplier, rh, projectSpace);

    HgWriterCreator wc = new HgWriterCreator(tipCloneSupplier, rh, projectSpace);

    return new Repository(name, rh, cc, wc);
  }

  static String runHgCommand(List<String> args, String workingDirectory)
      throws CommandRunner.CommandException {
    return AppContext.RUN.cmd.runCommand("hg", args, "", workingDirectory);
  }
}
