// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;
import java.io.File;
import java.util.List;
import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnWriterTest extends TestCase {

  private void expectSvnCommand(List<String> args, String workingDirectory, String result,
                                CommandRunner cmd) {
    ImmutableList.Builder<String> withAuthArgs = new ImmutableList.Builder<String>();
    withAuthArgs.add("--no-auth-cache").addAll(args);
    try {
      expect(cmd.runCommand("svn", withAuthArgs.build(), "", workingDirectory)).andReturn(result);
    } catch (Exception e) {}
  }

  private File f(String filename) {
    return new File(filename);
  }

  private CodebaseExpression e(String creatorIdentifier,
                               ImmutableMap<String, String> creatorOptions) {
    return new CodebaseExpression(
        new Term(creatorIdentifier, creatorOptions));
  }

  public void testPutEmptyCodebase() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    SvnRevisionHistory revisionHistory = control.createMock(SvnRevisionHistory.class);
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    expect(fileSystem.findFiles(f("/codebase"))).andReturn(ImmutableSet.<File>of());
    expect(fileSystem.findFiles(f("/writer"))).andReturn(
        ImmutableSet.<File>of(f("/writer/.svn/")));

    control.replay();
    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter("", null, f("/writer"), "public");
    DraftRevision r = e.putCodebase(c);
    control.verify();
    assertEquals("/writer", r.getLocation());
  }

  public void testWrongProjectSpace() throws Exception {
    AppContextForTesting.initForTest();
    Codebase c = new Codebase(f("/codebase"), "internal",
                              e("internal", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter("", null, f("/writer"), "public");
    try {
      DraftRevision r = e.putCodebase(c);
      fail();
    } catch (MoeProblem p) {}
  }

  public void testDeletedFile() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    SvnRevisionHistory revisionHistory = control.createMock(SvnRevisionHistory.class);
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    expect(fileSystem.exists(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.exists(f("/writer/foo"))).andReturn(true);

    expect(fileSystem.isExecutable(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.isExecutable(f("/writer/foo"))).andReturn(false);
    expectSvnCommand(ImmutableList.of("rm", "foo"), "/writer", "", cmd);
    control.replay();
    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter("", null, f("/writer"), "public");
    e.putFile("foo", c);
    control.verify();
  }

  public void testMoveContents() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    SvnRevisionHistory revisionHistory = control.createMock(SvnRevisionHistory.class);
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    expect(fileSystem.exists(f("/codebase/foo"))).andReturn(true);
    expect(fileSystem.exists(f("/writer/foo"))).andReturn(true);

    expect(fileSystem.isExecutable(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.isExecutable(f("/writer/foo"))).andReturn(false);
    fileSystem.makeDirsForFile(f("/writer/foo"));
    fileSystem.copyFile(f("/codebase/foo"), f("/writer/foo"));
    control.replay();
    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter("", null, f("/writer"), "public");
    e.putFile("foo", c);
    control.verify();
  }

  public void testNewfile() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    SvnRevisionHistory revisionHistory = control.createMock(SvnRevisionHistory.class);
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    expect(fileSystem.exists(f("/codebase/foo"))).andReturn(true);
    expect(fileSystem.exists(f("/writer/foo"))).andReturn(false);

    expect(fileSystem.isExecutable(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.isExecutable(f("/writer/foo"))).andReturn(false);
    fileSystem.makeDirsForFile(f("/writer/foo"));
    fileSystem.copyFile(f("/codebase/foo"), f("/writer/foo"));
    expectSvnCommand(ImmutableList.of("add", "--parents", "foo"), "/writer", "", cmd);
    control.replay();
    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter("", null, f("/writer"), "public");
    e.putFile("foo", c);
    control.verify();
  }

  public void testSetExecutable() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    SvnRevisionHistory revisionHistory = control.createMock(SvnRevisionHistory.class);
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    expect(fileSystem.exists(f("/codebase/foo"))).andReturn(true);
    expect(fileSystem.exists(f("/writer/foo"))).andReturn(true);

    expect(fileSystem.isExecutable(f("/codebase/foo"))).andReturn(true);
    expect(fileSystem.isExecutable(f("/writer/foo"))).andReturn(false);
    fileSystem.makeDirsForFile(f("/writer/foo"));
    fileSystem.copyFile(f("/codebase/foo"), f("/writer/foo"));
    expectSvnCommand(ImmutableList.of("propset", "svn:executable", "*", "foo"), "/writer", "", cmd);
    control.replay();
    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter("", null, f("/writer"), "public");
    e.putFile("foo", c);
    control.verify();
  }

  public void testDeleteExecutable() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    SvnRevisionHistory revisionHistory = control.createMock(SvnRevisionHistory.class);
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    expect(fileSystem.exists(f("/codebase/foo"))).andReturn(true);
    expect(fileSystem.exists(f("/writer/foo"))).andReturn(true);

    expect(fileSystem.isExecutable(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.isExecutable(f("/writer/foo"))).andReturn(true);
    fileSystem.makeDirsForFile(f("/writer/foo"));
    fileSystem.copyFile(f("/codebase/foo"), f("/writer/foo"));
    expectSvnCommand(ImmutableList.of("propdel", "svn:executable", "foo"), "/writer", "", cmd);
    control.replay();
    Codebase c = new Codebase(
        f("/codebase"), "public",
        e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter("", null, f("/writer"), "public");
    e.putFile("foo", c);
    control.verify();
  }

}
