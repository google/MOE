// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;

import java.io.File;

/**
 * An object encapsulating a cloned Hg repository, as backed by an actual clone to local disk
 *
 */
public class HgClonedRepository {

  private final String repositoryName;
  private final String repositoryUrl;
  private File localCloneTempDir;
  private boolean clonedLocally;

  // TODO(user): Support instantiation from an existing local repo (so that no actual cloning
  // occurs).
  /*package*/ HgClonedRepository(String repositoryName, String repositoryUrl) {
    this.repositoryName = repositoryName;
    this.repositoryUrl = repositoryUrl;
    clonedLocally = false;
  }

  /*package*/ String getRepositoryName() { return repositoryName; }

  /*package*/ String getRepositoryUrl() { return repositoryUrl; }

  /*package*/ File getLocalTempDir() {
    Preconditions.checkArgument(clonedLocally);
    return localCloneTempDir;
  }

  /*package*/ boolean isClonedLocally() { return clonedLocally; }

  /**
   * Clone repo at given URL to disk.
   */
  // TODO(user): Implement clean-up.
  /*package*/ void cloneLocally() {
    Preconditions.checkArgument(!clonedLocally);
    localCloneTempDir = AppContext.RUN.fileSystem.getTemporaryDirectory(
        String.format("hg_clone_%s_", repositoryName));

    try {
      HgRepository.runHgCommand(
          ImmutableList.<String>of(
              "clone",
              repositoryUrl,
              localCloneTempDir.getAbsolutePath()),
          "" /*workingDirectory*/);
      clonedLocally = true;
    } catch (CommandException e) {
      throw new MoeProblem("Could not clone from hg repo at " + repositoryUrl + ": " + e.stderr);
    }
  }
}
