// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

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
 *
 */
public class HgClonedRepositoryTest extends TestCase {

  public void testCloneLocally() {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();

    String repositoryName = "mockrepo";
    String repositoryURL = "http://foo/hg";
    String localCloneTempDir = "/tmp/hg_clone_mockrepo_12345";

    // Mock AppContext.RUN.fileSystem.getTemporaryDirectory()
    FileSystem mockFS = control.createMock(FileSystem.class);
    expect(mockFS.getTemporaryDirectory("hg_clone_" + repositoryName + "_"))
        .andReturn(new File(localCloneTempDir));
    AppContext.RUN.fileSystem = mockFS;

    // Mock HgRepository.runHgCommand()
    CommandRunner cmd = control.createMock(CommandRunner.class);
    try {
      expect(
          cmd.runCommand(
              "hg",
              ImmutableList.<String>of(
                  "clone",
                  "--update=tip",
                  repositoryURL,
                  localCloneTempDir),
              "" /*stdinData*/,
              "" /*workingDirectory*/))
          .andReturn("hg clone ok (mock output)");
    } catch (CommandException e) { throw new RuntimeException(e); }
    AppContext.RUN.cmd = cmd;

    // Run test
    control.replay();

    HgClonedRepository repo = new HgClonedRepository(repositoryName, repositoryURL);
    assertFalse(repo.isClonedLocally());

    repo.cloneLocallyAtTip();
    assertEquals(repositoryName, repo.getRepositoryName());
    assertEquals(repositoryURL, repo.getRepositoryUrl());
    assertEquals(localCloneTempDir, repo.getLocalTempDir().getAbsolutePath());
    assertTrue(repo.isClonedLocally());

    control.verify();
  }
}
