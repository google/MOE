// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.git;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.testing.ExtendedTestModule;

import dagger.ObjectGraph;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Unit tests for GitClonedRepository.
 */
public class GitClonedRepositoryTest extends TestCase {
  
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFS = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final RepositoryConfig repositoryConfig = control.createMock(RepositoryConfig.class);
  
  private final String repositoryName = "mockrepo";
  private final String repositoryURL = "http://foo/git";
  private final String localCloneTempDir = "/tmp/git_clone_mockrepo_12345";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ObjectGraph graph = ObjectGraph.create(new ExtendedTestModule(mockFS, cmd));
    graph.injectStatics();
 
    expect(repositoryConfig.getUrl()).andReturn(repositoryURL).anyTimes();
    expect(repositoryConfig.getBranch()).andReturn(Optional.<String>absent()).anyTimes();
  }

  private void expectCloneLocally() throws Exception {
    // The Lifetimes of clones in these tests are arbitrary since we're not really creating any
    // temp dirs and we're not testing clean-up.
    expect(mockFS.getTemporaryDirectory(
        EasyMock.eq("git_clone_" + repositoryName + "_"), EasyMock.<Lifetime>anyObject()))
        .andReturn(new File(localCloneTempDir));

    expect(cmd.runCommand(
        "git",
        ImmutableList.<String>of("clone", repositoryURL, localCloneTempDir),
        "" /*workingDirectory*/))
        .andReturn("git clone ok (mock output)");
  }

  public void testCloneLocally() throws Exception {
    expectCloneLocally();

    control.replay();
    GitClonedRepository repo = new GitClonedRepository(repositoryName, repositoryConfig);
    repo.cloneLocallyAtHead(Lifetimes.persistent());
    assertEquals(repositoryName, repo.getRepositoryName());
    assertEquals(repositoryURL, repo.getConfig().getUrl());
    assertEquals(localCloneTempDir, repo.getLocalTempDir().getAbsolutePath());

    try {
      repo.cloneLocallyAtHead(Lifetimes.persistent());
      fail("Re-cloning repo succeeded unexpectedly.");
    } catch (IllegalStateException expected) {}

    control.verify();
  }

  public void testCloneLocally_branch() throws Exception {
    EasyMock.reset(repositoryConfig);
    expect(repositoryConfig.getUrl()).andReturn(repositoryURL).anyTimes();
    expect(repositoryConfig.getBranch()).andReturn(Optional.of("mybranch")).anyTimes();

    expect(mockFS.getTemporaryDirectory(
        EasyMock.eq("git_clone_" + repositoryName + "_"), EasyMock.<Lifetime>anyObject()))
        .andReturn(new File(localCloneTempDir));

    expect(cmd.runCommand(
        "git",
        ImmutableList.of("clone", repositoryURL, localCloneTempDir, "--branch", "mybranch"),
        "" /*workingDirectory*/))
        .andReturn("git clone ok (mock output)");

    control.replay();
    GitClonedRepository repo = new GitClonedRepository(repositoryName, repositoryConfig);
    repo.cloneLocallyAtHead(Lifetimes.persistent());
    assertEquals(repositoryName, repo.getRepositoryName());
    assertEquals(repositoryURL, repo.getConfig().getUrl());
    assertEquals(localCloneTempDir, repo.getLocalTempDir().getAbsolutePath());

    try {
      repo.cloneLocallyAtHead(Lifetimes.persistent());
      fail("Re-cloning repo succeeded unexpectedly.");
    } catch (IllegalStateException expected) {}

    control.verify();
  }

  public void testUpdateToRevId_nonHeadRevId() throws Exception {
    String updateRevId = "notHead";
    String headRevId = "head";

    expectCloneLocally();
    
    expect(cmd.runCommand(
        "git",
        ImmutableList.of("rev-parse", "HEAD"),
        localCloneTempDir)).andReturn(headRevId);

    // Updating to revision other than head, so create a branch.
    expect(cmd.runCommand(
        "git",
        ImmutableList.of(
            "checkout",
            updateRevId,
            "-b",
            GitClonedRepository.MOE_MIGRATIONS_BRANCH_PREFIX + updateRevId),
        "/tmp/git_clone_mockrepo_12345")).andReturn(headRevId);

    control.replay();
    GitClonedRepository repo = new GitClonedRepository(repositoryName, repositoryConfig);
    repo.cloneLocallyAtHead(Lifetimes.persistent());
    repo.updateToRevision(updateRevId);
    control.verify();
  }
  
  public void testUpdateToRevId_headRevId() throws Exception {
    String updateRevId = "head";
    String headRevId = "head";

    expectCloneLocally();
    
    expect(cmd.runCommand(
        "git",
        ImmutableList.of("rev-parse", "HEAD"),
        localCloneTempDir)).andReturn(headRevId);

    // No branch creation expected.

    control.replay();
    GitClonedRepository repo = new GitClonedRepository(repositoryName, repositoryConfig);
    repo.cloneLocallyAtHead(Lifetimes.persistent());
    repo.updateToRevision(updateRevId);
    control.verify();
  }
}
