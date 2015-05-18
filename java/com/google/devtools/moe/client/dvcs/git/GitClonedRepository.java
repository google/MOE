// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.git;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.LocalClone;
import com.google.devtools.moe.client.project.RepositoryConfig;

import java.io.File;
import java.io.IOException;

/**
 * Git implementation of {@link LocalClone}, i.e. a 'git clone' to local disk.
 */
public class GitClonedRepository implements LocalClone {

  /**
   * A prefix for branches MOE creates to write migrated changes. For example, if there have been
   * revisions in a to-repository since an equivalence revision, MOE won't try to merge or rebase
   * those changes -- instead, it will create a branch with this prefix from the equivalence
   * revision.
   */
  static final String MOE_MIGRATIONS_BRANCH_PREFIX = "moe_writing_branch_from_";


  private final String repositoryName;
  private final RepositoryConfig repositoryConfig;
  /**
   * The location to clone from. If snapshotting a locally modified Writer, this will _not_ be
   * the same as repositoryConfig.getUrl(). Otherwise, it will.
   */
  private final String repositoryUrl;
  private File localCloneTempDir;
  private boolean clonedLocally;
  /** The revision of this clone, a Git hash ID */
  private String revId;

  GitClonedRepository(String repositoryName, RepositoryConfig repositoryConfig) {
    this(repositoryName, repositoryConfig, repositoryConfig.getUrl());
  }

  GitClonedRepository(
      String repositoryName, RepositoryConfig repositoryConfig, String repositoryUrl) {
    this.repositoryName = repositoryName;
    this.repositoryConfig = repositoryConfig;
    this.repositoryUrl = repositoryUrl;
    this.clonedLocally = false;
  }

  @Override
  public String getRepositoryName() {
    return repositoryName;
  }

  @Override
  public RepositoryConfig getConfig() {
    return repositoryConfig;
  }

  @Override
  public File getLocalTempDir() {
    Preconditions.checkState(clonedLocally);
    Preconditions.checkNotNull(localCloneTempDir);
    return localCloneTempDir;
  }

  @Override
  public void cloneLocallyAtHead(Lifetime cloneLifetime) {
    Preconditions.checkState(!clonedLocally);

    String tempDirName = String.format("git_clone_%s_", repositoryName);
    localCloneTempDir =
        Injector.INSTANCE.fileSystem().getTemporaryDirectory(tempDirName, cloneLifetime);
    Optional<String> branchName = repositoryConfig.getBranch();

    try {
      ImmutableList.Builder<String> cloneArgs = ImmutableList.<String>builder();
      cloneArgs.add("clone", repositoryUrl, localCloneTempDir.getAbsolutePath());
      if (branchName.isPresent()) {
        cloneArgs.add("--branch", branchName.get());
      }
      GitRepositoryFactory.runGitCommand(cloneArgs.build(), "" /*workingDirectory*/);
      clonedLocally = true;
      this.revId = "HEAD";
    } catch (CommandException e) {
      throw new MoeProblem(
          "Could not clone from git repo at " + repositoryUrl + ": " + e.stderr);
    }
  }

  @Override
  public void updateToRevision(String revId) {
    Preconditions.checkState(clonedLocally);
    Preconditions.checkState("HEAD".equals(this.revId));
    try {
      String headHash = runGitCommand("rev-parse", "HEAD").trim();
      // If we are updating to a revision other than the branch's head, branch from that revision.
      // Otherwise, no update/checkout is necessary since we are already at the desired revId,
      // branch head.
      if (!headHash.equals(revId)) {
        runGitCommand("checkout", revId, "-b", MOE_MIGRATIONS_BRANCH_PREFIX + revId);
      }
      this.revId = revId;
    } catch (CommandException e) {
      throw new MoeProblem(
          "Could not update git repo at " + localCloneTempDir + ": " + e.stderr);
    }
  }

  @Override
  public File archiveAtRevision(String revId) {
    Preconditions.checkState(clonedLocally);
    if (Strings.isNullOrEmpty(revId)) {
      revId = "HEAD";
    }
    File archiveLocation =
        Injector.INSTANCE.fileSystem().getTemporaryDirectory(
        String.format("git_archive_%s_%s_", repositoryName, revId),
        Lifetimes.currentTask());
    // Using this just to get a filename.
    String tarballPath =
        Injector.INSTANCE.fileSystem()
            .getTemporaryDirectory(
        String.format("git_tarball_%s_%s.tar.", repositoryName, revId),
        Lifetimes.currentTask()).getAbsolutePath();
    try {

      // Git doesn't support archiving to a directory: it only supports
      // archiving to a tar.  The fastest way to do this would be to pipe the
      // output directly into tar, however, there's no option for that using
      // the classes we have. (michaelpb)
      runGitCommand(
          "archive",
          "--format=tar",
          "--output=" + tarballPath,
          revId);

      // Make the directory to untar into
      Injector.INSTANCE.fileSystem().makeDirs(archiveLocation);

      // Untar the tarball we just made
      Injector.INSTANCE.cmd().runCommand(
          "tar",
          ImmutableList.<String>of(
              "xf",
              tarballPath,
              "-C",
              archiveLocation.getAbsolutePath()),
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
  String runGitCommand(String... args) throws CommandException {
    return Injector.INSTANCE.cmd().runCommand(
        "git",
        ImmutableList.copyOf(args),
        getLocalTempDir().getAbsolutePath() /*workingDirectory*/);
  }
}
