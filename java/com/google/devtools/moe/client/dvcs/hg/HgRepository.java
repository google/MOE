// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Lifetimes;
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
  public static Repository makeHgRepositoryFromConfig(
      final String name, final RepositoryConfig config) throws InvalidProject {
    Preconditions.checkArgument(config.getType() == RepositoryType.hg);

    final String url = config.getUrl();
    if (url == null || url.isEmpty()) {
      throw new InvalidProject("Hg repository config missing \"url\".");
    }

    Supplier<HgClonedRepository> freshSupplier = new Supplier<HgClonedRepository>() {
      @Override public HgClonedRepository get() {
        HgClonedRepository tipClone = new HgClonedRepository(name, config);
        tipClone.cloneLocallyAtHead(Lifetimes.currentTask());
        return tipClone;
      }
    };

    // RevisionHistory and CodebaseCreator don't modify their clones, so they can use a shared,
    // memoized supplier.
    Supplier<HgClonedRepository> memoizedSupplier = Suppliers.memoize(
        new Supplier<HgClonedRepository>() {
          @Override public HgClonedRepository get() {
            HgClonedRepository tipClone = new HgClonedRepository(name, config);
            tipClone.cloneLocallyAtHead(Lifetimes.moeExecution());
            return tipClone;
          }
        });

    HgRevisionHistory rh = new HgRevisionHistory(memoizedSupplier);

    String projectSpace = config.getProjectSpace();
    if (projectSpace == null) {
      projectSpace = "public";
    }

    HgCodebaseCreator cc = new HgCodebaseCreator(memoizedSupplier, rh, projectSpace, name, config);

    HgWriterCreator wc = new HgWriterCreator(freshSupplier, rh);

    return new Repository(name, rh, cc, wc);
  }

  static String runHgCommand(List<String> args, String workingDirectory)
      throws CommandRunner.CommandException {
    return Injector.INSTANCE.cmd.runCommand("hg", args, workingDirectory);
  }
}
