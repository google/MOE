// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import com.google.common.base.Supplier;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.codebase.LocalWorkspace;
import com.google.devtools.moe.client.dvcs.AbstractDvcsCodebaseCreator;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RevisionHistory;

/**
 * Hg implementation of AbstractDvcsCodebaseCreator to handle local cloning.
 *
 */
public class HgCodebaseCreator extends AbstractDvcsCodebaseCreator {

  private final String repositoryName;
  private final RepositoryConfig config;

  public HgCodebaseCreator(
      CommandRunner cmd,
      FileSystem filesystem,
      Supplier<? extends LocalWorkspace> headCloneSupplier,
      RevisionHistory revisionHistory,
      String projectSpace,
      String repositoryName,
      RepositoryConfig config) {
    super(cmd, filesystem, headCloneSupplier, revisionHistory, projectSpace);
    this.repositoryName = repositoryName;
    this.config = config;
  }

  @Override
  protected LocalWorkspace cloneAtLocalRoot(String localroot) {
    HgClonedRepository clone =
        new HgClonedRepository(cmd, filesystem, repositoryName, config, localroot);
    clone.cloneLocallyAtHead(Lifetimes.currentTask());
    return clone;
  }
}
