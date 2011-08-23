// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.Collections;

/**
 * Unit tests for the GitCodebaseCreator.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitCodebaseCreatorTest extends TestCase {

  String repositoryName = "mockrepo";

  public void testCreate_noGivenRev() {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();

    GitClonedRepository mockRepo = control.createMock(GitClonedRepository.class);
    GitRevisionHistory mockRevHistory = control.createMock(GitRevisionHistory.class);

    String archiveTempDir = "/tmp/git_archive_mockrepo_head";

    expect(mockRevHistory.findHighestRevision(null))
        .andReturn(new Revision("mock head changeset ID", repositoryName));
    expect(mockRepo.archiveAtRevId("mock head changeset ID")).andReturn(new File(archiveTempDir));
    expect(mockRepo.getRepositoryName()).andReturn("mockrepo");

    // Run test.
    control.replay();

    GitCodebaseCreator cc = new GitCodebaseCreator(
        Suppliers.ofInstance(mockRepo), mockRevHistory, "public");
    Codebase codebase;

    codebase = cc.create(Collections.<String, String>emptyMap());

    assertEquals(new File(archiveTempDir), codebase.getPath());
    assertEquals("public", codebase.getProjectSpace());
    assertEquals("mockrepo", codebase.getExpression().toString());

    control.verify();
  }

  public void testCreate_givenRev() {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();

    GitClonedRepository mockRepo = control.createMock(GitClonedRepository.class);
    GitRevisionHistory mockRevHistory = control.createMock(GitRevisionHistory.class);

    String givenRev = "givenrev";
    String archiveTempDir = "/tmp/git_reclone_mockrepo_head_" + givenRev;

    expect(mockRevHistory.findHighestRevision(givenRev))
        .andReturn(new Revision(givenRev, repositoryName));
    expect(mockRepo.archiveAtRevId(givenRev)).andReturn(new File(archiveTempDir));
    expect(mockRepo.getRepositoryName()).andReturn("mockrepo");

    // Run test.
    control.replay();

    GitCodebaseCreator cc = new GitCodebaseCreator(
        Suppliers.ofInstance(mockRepo), mockRevHistory, "public");
    Codebase codebase;

    codebase = cc.create(ImmutableMap.of("revision", givenRev));

    assertEquals(new File(archiveTempDir), codebase.getPath());
    assertEquals("public", codebase.getProjectSpace());
    assertEquals("mockrepo(revision=" + givenRev + ")",
                 codebase.getExpression().toString());

    control.verify();
  }
}
