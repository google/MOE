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

package com.google.devtools.moe.client.dvcs.git;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.config.RepositoryConfig;
import java.io.File;
import java.util.List;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

/**
 * Unit tests for GitClonedRepository.
 */
public class GitClonedRepositoryTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFS = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final RepositoryConfig repositoryConfig = control.createMock(RepositoryConfig.class);
  private final Lifetimes lifetimes = new Lifetimes(new Ui(System.err));

  private final String repositoryName = "mockrepo";
  private final String repositoryURL = "http://foo/git";
  private final String localCloneTempDir = "/tmp/git_clone_mockrepo_12345";

  private String testBranch = "master";
  private boolean testIsShallow = false;
  private List<String> testSparse = ImmutableList.of();

  private void mockConfig() {
    expect(repositoryConfig.getUrl()).andReturn(repositoryURL).anyTimes();
    expect(repositoryConfig.getBranch()).andReturn(testBranch.equals("master")
        ? Optional.absent()
        : Optional.of(testBranch)).anyTimes();
    expect(repositoryConfig.shallowCheckout()).andReturn(testIsShallow).anyTimes();
    expect(repositoryConfig.getCheckoutPaths()).andReturn(testSparse).anyTimes();
  }

  private void expectCloneLocally() throws Exception {
    // The Lifetimes of clones in these tests are arbitrary since we're not really creating any
    // temp dirs and we're not testing clean-up.
    expect(mockFS.getTemporaryDirectory(EasyMock.eq(
        "git_clone_" + repositoryName + "_"
            + (testBranch.equals("master") ? "" : testBranch + "_")),
        EasyMock.anyObject())).andReturn(new File(localCloneTempDir));

    expect(cmd.runCommand("", "git", ImmutableList.of("init", localCloneTempDir)))
        .andReturn("git init ok (mock output)");

    expect(
            cmd.runCommand(
                localCloneTempDir,
                "git",
                ImmutableList.of("remote", "add", "origin", repositoryURL)))
        .andReturn("git add remote ok (mock output)");

    expect(cmd.runCommand(localCloneTempDir, "git", ImmutableList.of("fetch", "--tags")))
        .andReturn("git fetch --tags (mock output)");

    if (!testSparse.isEmpty()) {
      expect(
              cmd.runCommand(
                  localCloneTempDir,
                  "git",
                  ImmutableList.of("config", "core.sparseCheckout", "true")))
          .andReturn("git config ok (mock output)");
      mockFS.write(
          String.join("\n", testSparse) + "\n",
          new File(localCloneTempDir + "/.git/info/sparse-checkout"));
      expectLastCall();
    }

    if (testIsShallow) {
      expect(
              cmd.runCommand(
                  localCloneTempDir,
                  "git",
                  ImmutableList.of("pull", "--depth=1", "origin", testBranch)))
          .andReturn("git pull ok (mock output)");
    } else {
      expect(
              cmd.runCommand(
                  localCloneTempDir, "git", ImmutableList.of("pull", "origin", testBranch)))
          .andReturn("git pull ok (mock output)");
    }
  }

  private void runTestCloneLocally() throws Exception {
    mockConfig();
    expectCloneLocally();

    control.replay();
    GitClonedRepository repo =
        new GitClonedRepository(cmd, mockFS, repositoryName, repositoryConfig, lifetimes);
    repo.cloneLocallyAtHead(Lifetimes.persistent());
    assertEquals(repositoryName, repo.getRepositoryName());
    assertEquals(repositoryURL, repo.getConfig().getUrl());
    assertEquals(localCloneTempDir, repo.getLocalTempDir().getAbsolutePath());

    try {
      repo.cloneLocallyAtHead(Lifetimes.persistent());
      fail("Re-cloning repo succeeded unexpectedly.");
    } catch (IllegalStateException expected) {
    }

    control.verify();
  }

  public void testCloneLocally() throws Exception {
    runTestCloneLocally();
  }

  public void testCloneLocally_branch() throws Exception {
    testBranch = "mybranch";
    runTestCloneLocally();
  }

  public void testCloneLocally_shallow() throws Exception {
    testIsShallow = true;
    runTestCloneLocally();
  }

  public void testCloneLocally_sparse() throws Exception {
    testSparse = ImmutableList.of("test/path/*", "test/path2/*");
    runTestCloneLocally();
  }

  public void testUpdateToRevId_nonHeadRevId() throws Exception {
    mockConfig();

    String updateRevId = "notHead";
    String headRevId = "head";

    expectCloneLocally();

    expect(cmd.runCommand(localCloneTempDir, "git", ImmutableList.of("rev-parse", "HEAD")))
        .andReturn(headRevId);

    // Updating to revision other than head, so create a branch.
    expect(
            cmd.runCommand(
                localCloneTempDir,
                "git",
                ImmutableList.of(
                    "checkout",
                    updateRevId,
                    "-b",
                    GitClonedRepository.MOE_MIGRATIONS_BRANCH_PREFIX + updateRevId)))
        .andReturn(headRevId);

    control.replay();
    GitClonedRepository repo =
        new GitClonedRepository(cmd, mockFS, repositoryName, repositoryConfig, lifetimes);
    repo.cloneLocallyAtHead(Lifetimes.persistent());
    repo.updateToRevision(updateRevId);
    control.verify();
  }

  public void testUpdateToRevId_shallow() throws Exception {
    testIsShallow = true;
    mockConfig();
    String updateRevId = "notHead";
    String headRevId = "head";

    expectCloneLocally();

    expect(cmd.runCommand(localCloneTempDir, "git", ImmutableList.of("rev-parse", "HEAD")))
        .andReturn(headRevId);

    // Unshallow the repository first.
    expect(cmd.runCommand(localCloneTempDir, "git", ImmutableList.of("fetch", "--unshallow")))
        .andReturn("git fetch unshallow ok (mock output)");

    // Updating to revision other than head, so create a branch.
    expect(
            cmd.runCommand(
                localCloneTempDir,
                "git",
                ImmutableList.of(
                    "checkout",
                    updateRevId,
                    "-b",
                    GitClonedRepository.MOE_MIGRATIONS_BRANCH_PREFIX + updateRevId)))
        .andReturn(headRevId);

    control.replay();
    GitClonedRepository repo =
        new GitClonedRepository(cmd, mockFS, repositoryName, repositoryConfig, lifetimes);
    repo.cloneLocallyAtHead(Lifetimes.persistent());
    repo.updateToRevision(updateRevId);
    control.verify();
  }

  public void testUpdateToRevId_headRevId() throws Exception {
    mockConfig();
    String updateRevId = "head";
    String headRevId = "head";

    expectCloneLocally();

    expect(cmd.runCommand(localCloneTempDir, "git", ImmutableList.of("rev-parse", "HEAD")))
        .andReturn(headRevId);

    // No branch creation expected.

    control.replay();
    GitClonedRepository repo =
        new GitClonedRepository(cmd, mockFS, repositoryName, repositoryConfig, lifetimes);
    repo.cloneLocallyAtHead(Lifetimes.persistent());
    repo.updateToRevision(updateRevId);
    control.verify();
  }
}
