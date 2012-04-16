// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Unit tests for HgClonedRepository: verify that Hg cloning works as expected.
 *
 */
public class HgClonedRepositoryTest extends TestCase {

  public void testCloneLocally() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();

    String repositoryName = "mockrepo";
    String repositoryURL = "http://foo/hg";
    RepositoryConfig repositoryConfig = control.createMock(RepositoryConfig.class);
    expect(repositoryConfig.getUrl()).andReturn(repositoryURL).anyTimes();
    String localCloneTempDir = "/tmp/hg_clone_mockrepo_12345";

    // Mock AppContext.RUN.fileSystem.getTemporaryDirectory()
    FileSystem mockFS = control.createMock(FileSystem.class);
    // The Lifetimes of clones in these tests are arbitrary since we're not really creating any
    // temp dirs and we're not testing clean-up.
    expect(mockFS.getTemporaryDirectory(
        EasyMock.eq("hg_clone_" + repositoryName + "_"), EasyMock.<Lifetime>anyObject()))
        .andReturn(new File(localCloneTempDir));
    AppContext.RUN.fileSystem = mockFS;

    // Mock HgRepository.runHgCommand()
    CommandRunner cmd = control.createMock(CommandRunner.class);
    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "clone",
                "--update=" + HgRevisionHistory.HG_TIP_REVID,
                repositoryURL,
                localCloneTempDir),
            "" /*workingDirectory*/))
        .andReturn("hg clone ok (mock output)");
    AppContext.RUN.cmd = cmd;

    // Run test
    control.replay();

    HgClonedRepository repo = new HgClonedRepository(repositoryName, repositoryConfig);
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
}
