// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.writer.DraftRevision;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.joda.time.DateTime;

import java.io.File;

/**
 * Test HgWriter by expect()ing file system calls and hg commands to add/remove files.
 *
 */
// TODO(user): Create a FakeFileSystem to replace explicit mocked calls.
public class HgWriterTest extends TestCase {

  private static final File CODEBASE_ROOT = new File("/codebase");
  private static final File WRITER_ROOT = new File("/writer");
  private static final String PROJECT_SPACE = "public";
  private static final RepositoryExpression CODEBASE_EXPR = new RepositoryExpression(
      new Term(PROJECT_SPACE, ImmutableMap.<String, String>of()));

  private IMocksControl control;
  private FileSystem mockFs;
  private CommandRunner mockCmd;
  private Codebase codebase;
  private HgClonedRepository mockRevClone;
  private RepositoryConfig mockRepoConfig;

  /* Helper methods */

  private void expectHgCmd(String... args) throws CommandException {
    expect(mockRevClone.runHgCommand(args)).andReturn("" /*stdout*/);
  }

  /* End helper methods */

  @Override protected void setUp() throws Exception {
    super.setUp();
    AppContextForTesting.initForTest();
    control = EasyMock.createControl();
    mockFs = control.createMock(FileSystem.class);
    mockCmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = mockCmd;
    AppContext.RUN.fileSystem = mockFs;
    codebase = new Codebase(CODEBASE_ROOT, PROJECT_SPACE, CODEBASE_EXPR);
    mockRevClone = control.createMock(HgClonedRepository.class);
    mockRepoConfig = control.createMock(RepositoryConfig.class);

    expect(mockRevClone.getLocalTempDir()).andReturn(WRITER_ROOT).anyTimes();
    expect(mockRevClone.getConfig()).andReturn(mockRepoConfig).anyTimes();
    expect(mockRepoConfig.getIgnoreFileRes()).andReturn(ImmutableList.<String>of()).anyTimes();
    expect(mockRepoConfig.getProjectSpace()).andReturn(PROJECT_SPACE).anyTimes();
  }

  public void testPutCodebase_emptyCodebase() throws Exception {

    // Define the files in the codebase and in the writer (hg repo).
    expect(mockFs.findFiles(CODEBASE_ROOT)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(WRITER_ROOT)).andReturn(ImmutableSet.<File>of(
        new File(WRITER_ROOT, ".hg"),
        new File(WRITER_ROOT, ".hgignore"),
        new File(WRITER_ROOT, ".hg/branch"),
        new File(WRITER_ROOT, ".hg/cache/tags")));

    // Expect no other mockFs calls from HgWriter.putFile().

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    DraftRevision draftRevision = writer.putCodebase(codebase);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }

  public void testPutCodebase_addFile() throws Exception {

    expect(mockFs.findFiles(CODEBASE_ROOT)).andReturn(
        ImmutableSet.<File>of(new File(CODEBASE_ROOT, "file1")));
    expect(mockFs.findFiles(WRITER_ROOT)).andReturn(ImmutableSet.<File>of());

    expect(mockFs.exists(new File(CODEBASE_ROOT, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(WRITER_ROOT, "file1"))).andReturn(false);

    mockFs.makeDirsForFile(new File(WRITER_ROOT, "file1"));
    mockFs.copyFile(new File(CODEBASE_ROOT, "file1"), new File(WRITER_ROOT, "file1"));
    expectHgCmd("add", "file1");

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    DraftRevision draftRevision = writer.putCodebase(codebase);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }

  public void testPutCodebase_editFile() throws Exception {

    expect(mockFs.findFiles(CODEBASE_ROOT)).andReturn(
        ImmutableSet.<File>of(new File(CODEBASE_ROOT, "file1")));
    expect(mockFs.findFiles(WRITER_ROOT))
        .andReturn(ImmutableSet.<File>of(new File(WRITER_ROOT, "file1")));

    expect(mockFs.exists(new File(CODEBASE_ROOT, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(WRITER_ROOT, "file1"))).andReturn(true);

    mockFs.makeDirsForFile(new File(WRITER_ROOT, "file1"));
    mockFs.copyFile(new File(CODEBASE_ROOT, "file1"), new File(WRITER_ROOT, "file1"));

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    DraftRevision draftRevision = writer.putCodebase(codebase);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }

  public void testPutCodebase_removeFile() throws Exception {

    expect(mockFs.findFiles(CODEBASE_ROOT)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(WRITER_ROOT))
        .andReturn(ImmutableSet.<File>of(new File(WRITER_ROOT, "file1")));

    expect(mockFs.exists(new File(CODEBASE_ROOT, "file1"))).andReturn(false);
    expect(mockFs.exists(new File(WRITER_ROOT, "file1"))).andReturn(true);

    expectHgCmd("rm", "file1");

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    DraftRevision draftRevision = writer.putCodebase(codebase);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }

  public void testPutCodebase_editFileWithMetadata() throws Exception {
    expect(mockFs.findFiles(CODEBASE_ROOT)).andReturn(
        ImmutableSet.<File>of(new File(CODEBASE_ROOT, "file1")));
    expect(mockFs.findFiles(WRITER_ROOT))
        .andReturn(ImmutableSet.<File>of(new File(WRITER_ROOT, "file1")));

    expect(mockFs.exists(new File(CODEBASE_ROOT, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(WRITER_ROOT, "file1"))).andReturn(true);

    mockFs.makeDirsForFile(new File(WRITER_ROOT, "file1"));
    mockFs.copyFile(new File(CODEBASE_ROOT, "file1"), new File(WRITER_ROOT, "file1"));

    expectHgCmd("status");

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    RevisionMetadata revisionMetadata =
        new RevisionMetadata("rev1", "author", new DateTime(1L),
            "desc", ImmutableList.<Revision>of());
    DraftRevision draftRevision = writer.putCodebase(codebase, revisionMetadata);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }
}
