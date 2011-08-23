// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.writer.DraftRevision;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Test GitWriter by expect()ing file system calls and git commands to add/remove files.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitWriterTest extends TestCase {

  IMocksControl control;
  FileSystem mockFs;
  Codebase codebase;
  GitClonedRepository mockRevClone;

  final File codebaseRoot = new File("/codebase");
  final File writerRoot = new File("/writer");
  final String projectSpace = "public";
  final CodebaseExpression cExp = new CodebaseExpression(
      new Term(projectSpace, ImmutableMap.<String, String>of()));

  /* Helper methods */

  private void expectGitCmd(String... args) throws CommandException {
    expect(mockRevClone.runGitCommand(ImmutableList.copyOf(args)))
        .andReturn("" /* stdout */);
  }

  /* End helper methods */

  @Override public void setUp() {
    AppContextForTesting.initForTest();
    control = EasyMock.createControl();
    mockFs = control.createMock(FileSystem.class);
    AppContext.RUN.fileSystem = mockFs;
    codebase = new Codebase(codebaseRoot, projectSpace, cExp);
    mockRevClone = control.createMock(GitClonedRepository.class);

    expect(mockRevClone.getLocalTempDir()).andReturn(writerRoot).anyTimes();
  }

  public void testPutCodebase_emptyCodebase() throws Exception {
    // Define the files in the codebase and in the writer (git repo).
    expect(mockFs.findFiles(codebaseRoot)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(writerRoot)).andReturn(ImmutableSet.<File>of(
        // Doesn't seem to matter that much what we return here, other than .git.
        new File(writerRoot, ".git/branches")));

    // Expect no other mockFs calls from GitWriter.putFile().

    control.replay();

    GitWriter w = new GitWriter(Suppliers.ofInstance(mockRevClone), projectSpace);
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
    expectGitCmd("add", "file1");

    control.replay();

    GitWriter w = new GitWriter(Suppliers.ofInstance(mockRevClone), projectSpace);
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

    GitWriter w = new GitWriter(Suppliers.ofInstance(mockRevClone), projectSpace);
    DraftRevision dr = w.putCodebase(codebase);

    control.verify();
  }

  public void testPutCodebase_removeFile() throws Exception {
    expect(mockFs.findFiles(codebaseRoot)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(writerRoot))
        .andReturn(ImmutableSet.<File>of(new File(writerRoot, "file1")));

    expect(mockFs.exists(new File(codebaseRoot, "file1"))).andReturn(false);
    expect(mockFs.exists(new File(writerRoot, "file1"))).andReturn(true);

    expectGitCmd("rm", "file1");

    control.replay();

    GitWriter w = new GitWriter(Suppliers.ofInstance(mockRevClone), projectSpace);
    DraftRevision dr = w.putCodebase(codebase);

    control.verify();
  }
}
