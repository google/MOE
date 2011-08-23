// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Unit tests for GitClonedRepository.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitClonedRepositoryTest extends TestCase {

  public void testCloneLocally() {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();

    String repositoryName = "mockrepo";
    String repositoryURL = "http://foo/git";
    String localCloneTempDir = "/tmp/git_clone_mockrepo_12345";

    // Mock AppContext.RUN.fileSystem.getTemporaryDirectory().
    FileSystem mockFS = control.createMock(FileSystem.class);
    expect(mockFS.getTemporaryDirectory("git_clone_" + repositoryName + "_"))
        .andReturn(new File(localCloneTempDir));
    AppContext.RUN.fileSystem = mockFS;

    // Mock GitRepository.runGitCommand().
    CommandRunner cmd = control.createMock(CommandRunner.class);
    try {
      expect(
          cmd.runCommand(
              "git",
              ImmutableList.<String>of(
                  "clone",
                  repositoryURL,
                  localCloneTempDir),
              "" /*stdinData*/,
              "" /*workingDirectory*/))
          .andReturn("git clone ok (mock output)");
    } catch (CommandException e) { throw new RuntimeException(e); }
    AppContext.RUN.cmd = cmd;

    // Run test.
    control.replay();

    GitClonedRepository repo = new GitClonedRepository(repositoryName, repositoryURL);
    assertFalse(repo.isClonedLocally());

    repo.cloneLocallyAtHead();
    assertEquals(repositoryName, repo.getRepositoryName());
    assertEquals(repositoryURL, repo.getRepositoryUrl());
    assertEquals(localCloneTempDir, repo.getLocalTempDir().getAbsolutePath());
    assertTrue(repo.isClonedLocally());

    control.verify();
  }
}
