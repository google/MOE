// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.writer.DraftRevision;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Test HgWriter by expect()ing file system calls and hg commands to add/remove files.
 *
 */
public class HgWriterTest extends TestCase {

  IMocksControl control;
  FileSystem mockFs;
  CommandRunner mockCmd;
  Codebase codebase;
  HgClonedRepository mockRevClone;

  final File codebaseRoot = new File("/codebase");
  final File writerRoot = new File("/writer");
  final String projectSpace = "public";
  final CodebaseExpression cExp = new CodebaseExpression(
      new Term(projectSpace, ImmutableMap.<String, String>of()));

  /* Helper methods */

  private void expectHgCmd(String... args) throws CommandException {
    expect(mockCmd.runCommand("hg", ImmutableList.copyOf(args), "", writerRoot.getAbsolutePath()))
        .andReturn("" /*stdout*/);
  }

  /* End helper methods */

  @Override public void setUp() {
    AppContextForTesting.initForTest();
    control = EasyMock.createControl();
    mockFs = control.createMock(FileSystem.class);
    mockCmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = mockCmd;
    AppContext.RUN.fileSystem = mockFs;
    codebase = new Codebase(codebaseRoot, projectSpace, cExp);
    mockRevClone = control.createMock(HgClonedRepository.class);

    expect(mockRevClone.getLocalTempDir()).andReturn(writerRoot).anyTimes();
  }

  public void testPutCodebase_emptyCodebase() throws Exception {

    // Define the files in the codebase and in the writer (hg repo).
    expect(mockFs.findFiles(codebaseRoot)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(writerRoot)).andReturn(ImmutableSet.<File>of(
        new File(writerRoot, ".hg"),
        new File(writerRoot, ".hgignore"),
        new File(writerRoot, ".hg/branch"),
        new File(writerRoot, ".hg/cache/tags")));

    // Expect no other mockFs calls from HgWriter.putFile().

    control.replay();

    HgWriter w = new HgWriter(mockRevClone, projectSpace);
    DraftRevision dr = w.putCodebase(codebase);

    control.verify();

    assertEquals(writerRoot.getAbsolutePath(), dr.getLocation());
  }

  public void testPutCodebase_addFile() throws Exception {

    expect(mockFs.findFiles(codebaseRoot)).andReturn(
        ImmutableSet.<File>of(new File(codebaseRoot, "file1")));
    expect(mockFs.findFiles(writerRoot)).andReturn(ImmutableSet.<File>of());

    expect(mockFs.exists(new File(codebaseRoot, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(writerRoot, "file1"))).andReturn(false);

    mockFs.makeDirsForFile(new File(writerRoot, "file1"));
    mockFs.copyFile(new File(codebaseRoot, "file1"), new File(writerRoot, "file1"));
    expectHgCmd("add", "file1");

    control.replay();

    HgWriter w = new HgWriter(mockRevClone, projectSpace);
    DraftRevision dr = w.putCodebase(codebase);

    control.verify();
  }

  public void testPutCodebase_editFile() throws Exception {

    expect(mockFs.findFiles(codebaseRoot)).andReturn(
        ImmutableSet.<File>of(new File(codebaseRoot, "file1")));
    expect(mockFs.findFiles(writerRoot))
        .andReturn(ImmutableSet.<File>of(new File(writerRoot, "file1")));

    expect(mockFs.exists(new File(codebaseRoot, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(writerRoot, "file1"))).andReturn(true);

    mockFs.makeDirsForFile(new File(writerRoot, "file1"));
    mockFs.copyFile(new File(codebaseRoot, "file1"), new File(writerRoot, "file1"));

    control.replay();

    HgWriter w = new HgWriter(mockRevClone, projectSpace);
    DraftRevision dr = w.putCodebase(codebase);

    control.verify();
  }

  public void testPutCodebase_removeFile() throws Exception {

    expect(mockFs.findFiles(codebaseRoot)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(writerRoot))
        .andReturn(ImmutableSet.<File>of(new File(writerRoot, "file1")));

    expect(mockFs.exists(new File(codebaseRoot, "file1"))).andReturn(false);
    expect(mockFs.exists(new File(writerRoot, "file1"))).andReturn(true);

    expectHgCmd("rm", "file1");

    control.replay();

    HgWriter w = new HgWriter(mockRevClone, projectSpace);
    DraftRevision dr = w.putCodebase(codebase);

    control.verify();
  }

  public void testPutCodebase_editFileWithMetadata() throws Exception {
    expect(mockFs.findFiles(codebaseRoot)).andReturn(
        ImmutableSet.<File>of(new File(codebaseRoot, "file1")));
    expect(mockFs.findFiles(writerRoot))
        .andReturn(ImmutableSet.<File>of(new File(writerRoot, "file1")));

    expect(mockFs.exists(new File(codebaseRoot, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(writerRoot, "file1"))).andReturn(true);

    mockFs.makeDirsForFile(new File(writerRoot, "file1"));
    mockFs.copyFile(new File(codebaseRoot, "file1"), new File(writerRoot, "file1"));

    File script = new File("/writer/hg_commit.sh");
    mockFs.write("#!/bin/sh\nhg commit -m \"desc\" -u \"author\"\nhg push", script);
    mockFs.setExecutable(script);
    control.replay();

    HgWriter w = new HgWriter(mockRevClone, projectSpace);
    RevisionMetadata rm = new RevisionMetadata("rev1", "author", "data", "desc",
                                               ImmutableList.<Revision>of());
    DraftRevision dr = w.putCodebase(codebase, rm);

    control.verify();
  }
}
