// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * An object encapsulating a cloned Git repository at HEAD, as backed by an actual 'git clone' to 
 * local disk
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitClonedRepository {

  private final String repositoryName;
  private final String repositoryUrl;
  private File localCloneTempDir;
  private boolean clonedLocally;
  /** The revision of this clone, a Git hash ID */
  private String revId;

  GitClonedRepository(String repositoryName, String repositoryUrl) {
    this.repositoryName = repositoryName;
    this.repositoryUrl = repositoryUrl;
    clonedLocally = false;
    revId = null;
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

  void cloneLocallyAtHead() {
    Preconditions.checkState(!clonedLocally);
    localCloneTempDir = AppContext.RUN.fileSystem.getTemporaryDirectory(
        String.format("git_clone_%s_", repositoryName));
    try {
      GitRepository.runGitCommand(
          ImmutableList.<String>of(
              "clone",
              repositoryUrl,
              localCloneTempDir.getAbsolutePath()),
          "" /*workingDirectory*/);
      clonedLocally = true;
      this.revId = "HEAD";
    } catch (CommandException e) {
      throw new MoeProblem("Could not clone from git repo at " + repositoryUrl + ": " + e.stderr);
    }
  }

  void updateToRevId(String revId) {
    Preconditions.checkState(clonedLocally);
    Preconditions.checkState("HEAD".equals(this.revId));
    try {
      runGitCommand(ImmutableList.<String>of("checkout", revId));
      this.revId = revId;
    } catch (CommandException e) {
      throw new MoeProblem("Could not clone from git repo at " + repositoryUrl + ": " + e.stderr);
    }
  }

  /**
   * Archive this repo (currently at HEAD) to local disk at a given revision.
   *
   * @param revId the hash ID at which to make a clone of this repo (non-null, non-empty)
   * @return the location of the archive
   */
  File archiveAtRevId(String revId) {
    Preconditions.checkState(clonedLocally);
    Preconditions.checkArgument(revId != null && !revId.isEmpty());
    File archiveLocation = AppContext.RUN.fileSystem.getTemporaryDirectory(
        String.format("git_archive_%s_%s_", repositoryName, revId));
    // Using this just to get a filename.
    String tarballPath = AppContext.RUN.fileSystem.getTemporaryDirectory(
        String.format("git_tarball_%s_%s.tar.", repositoryName, revId)).getAbsolutePath();
    try {

      // Git doesn't support archiving to a directory: it only supports
      // archiving to a tar.  The fastest way to do this would be to pipe the
      // output directly into tar, however, there's no option for that using
      // the classes we have. (michaelpb)
      runGitCommand(
          ImmutableList.<String>of(
              "archive",
              "--format=tar",
              "--output=" + tarballPath,
              revId));

      // Make the directory to untar into
      AppContext.RUN.fileSystem.makeDirs(archiveLocation);

      // Untar the tarball we just made
      AppContext.RUN.cmd.runCommand(
          "tar", 
          ImmutableList.<String>of(
              "xf",
              tarballPath,
              "-C",
              archiveLocation.getAbsolutePath()),
          "",
          "");
    
    } catch (CommandException e) {
      throw new MoeProblem(
          "Could not archive git clone at " + 
            localCloneTempDir.getAbsolutePath() + ": " + e.stderr);
    } catch (IOException e) {
      throw new MoeProblem(
          "IOException archiving clone at " + 
              localCloneTempDir.getAbsolutePath() +
              " to revision " + revId + ": " + e);
    }
    return archiveLocation;
  }

  /**
   * Runs a git command with the given arguments, in this cloned repository's directory.
   * 
   * @param args a list of arguments for git
   * @return a string containing the STDOUT result
   */
  String runGitCommand(List<String> args) throws CommandException {
    return AppContext.RUN.cmd.runCommand("git", args, "", 
        getLocalTempDir().getAbsolutePath() /*workingDirectory*/);
  }
}
