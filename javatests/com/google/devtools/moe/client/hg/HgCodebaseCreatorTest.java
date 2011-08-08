// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.Collections;

/**
 */
public class HgCodebaseCreatorTest extends TestCase {

  String repositoryName = "mockrepo";

  public void testCreate_noGivenRev() {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();

    HgClonedRepository mockRepo = control.createMock(HgClonedRepository.class);
    HgRevisionHistory mockRevHistory = control.createMock(HgRevisionHistory.class);

    String archiveTempDir = "/tmp/hg_archive_mockrepo_tip";

    expect(mockRevHistory.findHighestRevision(null))
        .andReturn(new Revision("mock tip changeset ID", repositoryName));
    expect(mockRepo.archiveAtRevId("mock tip changeset ID")).andReturn(new File(archiveTempDir));
    expect(mockRepo.getRepositoryName()).andReturn("mockrepo");

    // Run test.
    control.replay();

    HgCodebaseCreator cc = new HgCodebaseCreator(mockRepo, mockRevHistory, "public");
    Codebase codebase;
    try {
      codebase = cc.create(Collections.<String, String>emptyMap());
    } catch (CodebaseCreationError e) {
      throw new RuntimeException(e);
    }

    assertEquals(new File(archiveTempDir), codebase.getPath());
    assertEquals("public", codebase.getProjectSpace());
    assertEquals("mockrepo", codebase.getExpression().toString());

    control.verify();
  }

  public void testCreate_givenRev() {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();

    HgClonedRepository mockRepo = control.createMock(HgClonedRepository.class);
    HgRevisionHistory mockRevHistory = control.createMock(HgRevisionHistory.class);

    String givenRev = "givenrev";
    String archiveTempDir = "/tmp/hg_reclone_mockrepo_tip_" + givenRev;

    expect(mockRevHistory.findHighestRevision(givenRev))
        .andReturn(new Revision(givenRev, repositoryName));
    expect(mockRepo.archiveAtRevId(givenRev)).andReturn(new File(archiveTempDir));
    expect(mockRepo.getRepositoryName()).andReturn("mockrepo");

    // Run test.
    control.replay();

    HgCodebaseCreator cc = new HgCodebaseCreator(mockRepo, mockRevHistory, "public");
    Codebase codebase;
    try {
      codebase = cc.create(ImmutableMap.of("revision", givenRev));
    } catch (CodebaseCreationError e) {
      throw new RuntimeException(e);
    }

    assertEquals(new File(archiveTempDir), codebase.getPath());
    assertEquals("public", codebase.getProjectSpace());
    assertEquals("mockrepo(revision=" + givenRev + ")",
                 codebase.getExpression().toString());

    control.verify();
  }
}
