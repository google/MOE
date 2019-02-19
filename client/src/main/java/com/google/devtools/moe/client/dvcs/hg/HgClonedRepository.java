/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.dvcs.hg;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.LocalWorkspace;
import com.google.devtools.moe.client.config.RepositoryConfig;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Hg implementation of LocalClone, i.e. an 'hg clone' to local disk.
 */
public class HgClonedRepository implements LocalWorkspace {

  private final CommandRunner cmd;
  private final FileSystem filesystem;
  private final File hgBinary;
  private final String repositoryName;
  private final RepositoryConfig repositoryConfig;
  private final Lifetimes lifetimes;

  /**
   * The location to clone from. If snapshotting a locally modified Writer, this will <em>not</em>
   * be the same as repositoryConfig.getUrl(). Otherwise, it will.
   */
  private final String repositoryUrl;

  private File localCloneTempDir;
  private boolean clonedLocally;
  private boolean updatedToRev = false;
  private String branch = null;

  public HgClonedRepository(
      CommandRunner cmd,
      FileSystem filesystem,
      File hgBinary,
      String repositoryName,
      RepositoryConfig repositoryConfig,
      Lifetimes lifetimes) {
    this(
        cmd,
        filesystem,
        hgBinary,
        repositoryName,
        repositoryConfig,
        repositoryConfig.getUrl(),
        lifetimes);
  }

  HgClonedRepository(
      CommandRunner cmd,
      FileSystem filesystem,
      File hgBinary,
      String repositoryName,
      RepositoryConfig repositoryConfig,
      String repositoryUrl,
      Lifetimes lifetimes) {
    this.cmd = cmd;
    this.filesystem = filesystem;
    this.hgBinary = hgBinary;
    this.repositoryName = repositoryName;
    this.repositoryConfig = repositoryConfig;
    this.repositoryUrl = repositoryUrl;
    this.clonedLocally = false;
    this.lifetimes = lifetimes;
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
    Preconditions.checkState(localCloneTempDir != null);
    return localCloneTempDir;
  }

  String getBranch() {
    Preconditions.checkState(clonedLocally);
    Preconditions.checkNotNull(branch);
    return branch;
  }

  @Override
  public void cloneLocallyAtHead(Lifetime cloneLifetime) {
    Preconditions.checkState(!clonedLocally);

    String tempDirName = "hg_clone_" + repositoryName + "_";
    localCloneTempDir = filesystem.getTemporaryDirectory(tempDirName, cloneLifetime);

    try {
      Optional<String> branchName = repositoryConfig.getBranch();
      ImmutableList.Builder<String> cloneArgs = ImmutableList.<String>builder();
      cloneArgs.add("clone", repositoryUrl, localCloneTempDir.getAbsolutePath());
      if (branchName.isPresent()) {
        cloneArgs.add("--rev=" + branchName.get());
      }

      runHgCommand(null, cloneArgs.build());
      clonedLocally = true;
      branch = runHgCommand(localCloneTempDir, ImmutableList.of("branch")).trim();
    } catch (CommandException e) {
      throw new MoeProblem(e, "Could not clone from hg repo at %s: %s", repositoryUrl, e.stderr);
    }
  }

  @Override
  public void updateToRevision(String revId) {
    Preconditions.checkState(clonedLocally);
    Preconditions.checkState(!updatedToRev);
    try {
      runHgCommand(getLocalTempDir(), ImmutableList.of("update", revId));
      updatedToRev = true;
    } catch (CommandException e) {
      throw new MoeProblem(
          e, "Could not clone from hg repo at %s: %s", localCloneTempDir, e.stderr);
    }
  }

  @Override
  public File archiveAtRevision(String revId) {
    Preconditions.checkState(clonedLocally);
    File archiveLocation =
        filesystem.getTemporaryDirectory(
            String.format("hg_archive_%s_%s_", repositoryName, revId), lifetimes.currentTask());
    try {
      ImmutableList.Builder<String> archiveArgs = ImmutableList.<String>builder();
      archiveArgs.add("archive", archiveLocation.getAbsolutePath());
      if (!Strings.isNullOrEmpty(revId)) {
        archiveArgs.add("--rev=" + revId);
      }
      runHgCommand(localCloneTempDir, archiveArgs.build());
      filesystem.deleteRecursively(new File(archiveLocation, ".hg_archival.txt"));
    } catch (CommandException e) {
      throw new MoeProblem(
          e, "Could not archive hg clone at %s: %s", localCloneTempDir.getAbsolutePath(), e.stderr);
    } catch (IOException e) {
      throw new MoeProblem(
          e,
          "Failed to archive clone at %s to revision %s",
          localCloneTempDir.getAbsolutePath(),
          revId);
    }
    return archiveLocation;
  }

  /**
   * Runs an hg command with the given arguments, in this cloned repository's directory.
   *
   * @param workingDirectory the working directory in which the command should be run, if any
   * @param args  a list of arguments to the 'hg' command
   * @return the stdout output of the command
   */
  String runHgCommand(File workingDirectory, List<String> args) throws CommandException {
    return cmd.runCommand(
        workingDirectory == null ? null : workingDirectory.getAbsolutePath(),
        hgBinary.getPath(),
        ImmutableList.copyOf(args));
  }
}
