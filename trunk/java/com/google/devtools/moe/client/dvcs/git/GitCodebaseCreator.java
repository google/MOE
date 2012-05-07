// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.git;

import com.google.common.base.Supplier;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.codebase.LocalClone;
import com.google.devtools.moe.client.dvcs.AbstractDvcsCodebaseCreator;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RevisionHistory;

/**
 * Git implementation of AbstractDvcsCodebaseCreator to handle local cloning.
 *
 */
public class GitCodebaseCreator extends AbstractDvcsCodebaseCreator {

  private final String repositoryName;
  private final RepositoryConfig config;

  public GitCodebaseCreator(
      Supplier<? extends LocalClone> headCloneSupplier,
      RevisionHistory revisionHistory,
      String projectSpace,
      String repositoryName,
      RepositoryConfig config) {
    super(headCloneSupplier, revisionHistory, projectSpace);
    this.repositoryName = repositoryName;
    this.config = config;
  }

  @Override
  protected LocalClone cloneAtLocalRoot(String localroot) {
    GitClonedRepository clone = new GitClonedRepository(repositoryName, config, localroot);
    clone.cloneLocallyAtHead(Lifetimes.currentTask());
    return clone;
  }
}
