// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;

import java.util.List;

import javax.inject.Inject;

/**
 * Creates a Mercurial (hg) implementation of {@link RepositoryType}.
 */
public class HgRepositoryFactory implements RepositoryType.Factory {
  private final CommandRunner cmd;
  private final FileSystem filesystem;

  @Inject
  HgRepositoryFactory(CommandRunner cmd, FileSystem filesystem) {
    this.cmd = cmd;
    this.filesystem = filesystem;
  }

  @Override
  public String type() {
    return "hg";
  }

  /**
   * Create a Repository from a RepositoryConfig indicating an Hg repo ("type" == "hg").
   *
   * @throws InvalidProject if RepositoryConfig is missing a repo URL.
   */
  @Override
  public RepositoryType create(final String name, final RepositoryConfig config)
      throws InvalidProject {
    config.checkType(this);

    final String url = config.getUrl();
    if (isNullOrEmpty(url)) {
      throw new InvalidProject("Hg repository config missing \"url\".");
    }

    Supplier<HgClonedRepository> freshSupplier =
        new Supplier<HgClonedRepository>() {
          @Override
          public HgClonedRepository get() {
            HgClonedRepository tipClone = new HgClonedRepository(cmd, filesystem, name, config);
            tipClone.cloneLocallyAtHead(Lifetimes.currentTask());
            return tipClone;
          }
        };

    // RevisionHistory and CodebaseCreator don't modify their clones, so they can use a shared,
    // memoized supplier.
    Supplier<HgClonedRepository> memoizedSupplier =
        Suppliers.memoize(
            new Supplier<HgClonedRepository>() {
              @Override
              public HgClonedRepository get() {
                HgClonedRepository tipClone = new HgClonedRepository(cmd, filesystem, name, config);
                tipClone.cloneLocallyAtHead(Lifetimes.moeExecution());
                return tipClone;
              }
            });

    HgRevisionHistory rh = new HgRevisionHistory(memoizedSupplier);

    String projectSpace = config.getProjectSpace();
    if (projectSpace == null) {
      projectSpace = "public";
    }

    HgCodebaseCreator cc =
        new HgCodebaseCreator(cmd, filesystem, memoizedSupplier, rh, projectSpace, name, config);

    HgWriterCreator wc = new HgWriterCreator(freshSupplier, rh);

    return RepositoryType.create(name, rh, cc, wc);
  }

  static String runHgCommand(List<String> args, String workingDirectory) throws CommandException {
    return Injector.INSTANCE.cmd().runCommand("hg", args, workingDirectory);
  }
}
