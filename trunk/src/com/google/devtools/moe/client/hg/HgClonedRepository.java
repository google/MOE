// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;

import java.io.File;
import java.io.IOException;

/**
 * An object encapsulating a cloned Hg repository at tip, as backed by an actual 'hg clone' to local
 * disk
 *
 */
public class HgClonedRepository {

  private final String repositoryName;
  private final String repositoryUrl;
  private File localCloneTempDir;
  private boolean clonedLocally;

  // TODO(user): Support instantiation from an existing local repo (so that no actual cloning
  // occurs).
  HgClonedRepository(String repositoryName, String repositoryUrl) {
    this.repositoryName = repositoryName;
    this.repositoryUrl = repositoryUrl;
    clonedLocally = false;
  }

  String getRepositoryName() {
    return repositoryName;
  }

  String getRepositoryUrl() {
    return repositoryUrl;
  }

  File getLocalTempDir() {
    Preconditions.checkState(clonedLocally);
    return localCloneTempDir;
  }

  boolean isClonedLocally() {
    return clonedLocally;
  }

  void cloneLocally() {
    Preconditions.checkState(!clonedLocally);
    localCloneTempDir = AppContext.RUN.fileSystem.getTemporaryDirectory(
        String.format("hg_tipclone_%s_", repositoryName));
    try {
      HgRepository.runHgCommand(
          ImmutableList.<String>of(
              "clone",
              "--rev=tip",
              repositoryUrl,
              localCloneTempDir.getAbsolutePath()),
          "" /*workingDirectory*/);
      clonedLocally = true;
    } catch (CommandException e) {
      throw new MoeProblem("Could not clone from hg repo at " + repositoryUrl + ": " + e.stderr);
    }
  }

  /**
   * Archive this repo (currently at tip) to local disk at a given revision.
   *
   * @param revId the changeset ID at which to make a clone of this repo (non-null, non-empty)
   * @return the location of the archive
   */
  File archiveAtRevId(String revId) {
    Preconditions.checkState(clonedLocally);
    Preconditions.checkArgument(revId != null && !revId.isEmpty());
    File archiveLocation = AppContext.RUN.fileSystem.getTemporaryDirectory(
        String.format("hg_archive_%s_%s_", repositoryName, revId));
    try {
      HgRepository.runHgCommand(
          ImmutableList.<String>of(
              "archive",
              "--rev=" + revId,
              archiveLocation.getAbsolutePath()),
          localCloneTempDir.getAbsolutePath() /*workingDirectory*/);
      clonedLocally = true;

      AppContext.RUN.fileSystem.deleteRecursively(new File(archiveLocation, ".hg_archival.txt"));
    } catch (CommandException e) {
      throw new MoeProblem(
          "Could not archive hg clone at " + localCloneTempDir.getAbsolutePath() + ": " + e.stderr);
    } catch (IOException e) {
      throw new MoeProblem(
          "IOException archiving clone at " + localCloneTempDir.getAbsolutePath() +
          " to revision " + revId + ": " + e);
    }
    return archiveLocation;
  }
}
