// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.LocalClone;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.testing.ExtendedTestModule;

import dagger.ObjectGraph;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.Collections;

/**
 * Tests for AbstractDvcsCodebaseCreator, esp. that create() archives at the right revision.
 *
 */
public class AbstractDvcsCodebaseCreatorTest extends TestCase {
  private static final String MOCK_REPO_NAME = "mockrepo";

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFS = control.createMock(FileSystem.class);
  private final RepositoryConfig mockRepoConfig = control.createMock(RepositoryConfig.class);

  private final LocalClone mockRepo = control.createMock(LocalClone.class);
  private final RevisionHistory mockRevHistory = control.createMock(RevisionHistory.class);
  private final AbstractDvcsCodebaseCreator codebaseCreator =
      new AbstractDvcsCodebaseCreator(Suppliers.ofInstance(mockRepo), mockRevHistory, "public") {
        @Override protected LocalClone cloneAtLocalRoot(String localroot) {
          throw new UnsupportedOperationException();
        }
      };

  @Override protected void setUp() throws Exception {
    super.setUp();
    ObjectGraph graph = ObjectGraph.create(new ExtendedTestModule(mockFS, null));
    graph.injectStatics();

    expect(mockRepo.getConfig()).andReturn(mockRepoConfig).anyTimes();
    expect(mockRepoConfig.getIgnoreFileRes()).andReturn(ImmutableList.<String>of());
    expect(mockRepo.getRepositoryName()).andReturn(MOCK_REPO_NAME);
  }

  public void testCreate_noGivenRev() throws Exception {
    String archiveTempDir = "/tmp/git_archive_mockrepo_head";
    // Short-circuit Utils.filterFilesByPredicate(ignore_files_re).
    expect(AppContext.RUN.fileSystem.findFiles(new File(archiveTempDir)))
        .andReturn(ImmutableSet.<File>of());

    expect(mockRevHistory.findHighestRevision(null))
        .andReturn(new Revision("mock head changeset ID", MOCK_REPO_NAME));
    expect(mockRepo.archiveAtRevision("mock head changeset ID"))
        .andReturn(new File(archiveTempDir));

    control.replay();

    Codebase codebase = codebaseCreator.create(Collections.<String, String>emptyMap());

    assertEquals(new File(archiveTempDir), codebase.getPath());
    assertEquals("public", codebase.getProjectSpace());
    assertEquals("mockrepo", codebase.getExpression().toString());

    control.verify();
  }

  public void testCreate_givenRev() throws Exception {
    String givenRev = "givenrev";
    String archiveTempDir = "/tmp/git_reclone_mockrepo_head_" + givenRev;
    // Short-circuit Utils.filterFilesByPredicate(ignore_files_re).
    expect(AppContext.RUN.fileSystem.findFiles(new File(archiveTempDir)))
        .andReturn(ImmutableSet.<File>of());

    expect(mockRevHistory.findHighestRevision(givenRev))
        .andReturn(new Revision(givenRev, MOCK_REPO_NAME));
    expect(mockRepo.archiveAtRevision(givenRev)).andReturn(new File(archiveTempDir));

    control.replay();

    Codebase codebase = codebaseCreator.create(ImmutableMap.of("revision", givenRev));

    assertEquals(new File(archiveTempDir), codebase.getPath());
    assertEquals("public", codebase.getProjectSpace());
    assertEquals("mockrepo(revision=" + givenRev + ")", codebase.getExpression().toString());

    control.verify();
  }
}
