// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
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

import java.io.File;
import java.util.List;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnWriterTest extends TestCase {

  private IMocksControl control;
  private RepositoryConfig mockConfig;
  private FileSystem fileSystem;
  private CommandRunner cmd;

  @Override public void setUp() throws Exception {
    AppContextForTesting.initForTest();
    control = EasyMock.createControl();
    mockConfig = control.createMock(RepositoryConfig.class);
    expect(mockConfig.getUrl()).andReturn("http://foo/svn/trunk/").anyTimes();
    expect(mockConfig.getProjectSpace()).andReturn("public").anyTimes();
    expect(mockConfig.getIgnoreFileRes()).andReturn(ImmutableList.<String>of()).anyTimes();

    fileSystem = control.createMock(FileSystem.class);
    cmd = control.createMock(CommandRunner.class);

    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;
  }

  private void expectSvnCommand(List<String> args, String workingDirectory, String result,
                                CommandRunner cmd) {
    ImmutableList.Builder<String> withAuthArgs = new ImmutableList.Builder<String>();
    withAuthArgs.add("--no-auth-cache").addAll(args);
    try {
      expect(cmd.runCommand("svn", withAuthArgs.build(), workingDirectory)).andReturn(result);
    } catch (Exception e) {}
  }

  private File f(String filename) {
    return new File(filename);
  }

  private RepositoryExpression e(String creatorIdentifier,
                               ImmutableMap<String, String> creatorOptions) {
    return new RepositoryExpression(
        new Term(creatorIdentifier, creatorOptions));
  }

  public void testPutEmptyCodebase() throws Exception {
    expect(fileSystem.findFiles(f("/codebase"))).andReturn(ImmutableSet.<File>of());
    expect(fileSystem.findFiles(f("/writer"))).andReturn(
        ImmutableSet.<File>of(f("/writer/.svn/")));

    control.replay();
    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"));
    DraftRevision r = e.putCodebase(c);
    control.verify();
    assertEquals("/writer", r.getLocation());
  }

  public void testWrongProjectSpace() throws Exception {
    Codebase c = new Codebase(f("/codebase"), "internal",
                              e("internal", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"));
    try {
      DraftRevision r = e.putCodebase(c);
      fail();
    } catch (MoeProblem p) {}
  }

  public void testDeletedFile() throws Exception {
    expect(fileSystem.exists(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.exists(f("/writer/foo"))).andReturn(true);

    expect(fileSystem.isExecutable(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.isExecutable(f("/writer/foo"))).andReturn(false);
    expectSvnCommand(ImmutableList.of("rm", "foo"), "/writer", "", cmd);
    control.replay();
    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"));
    e.putFile("foo", c);
    control.verify();
  }

  public void testMoveContents() throws Exception {
    expect(fileSystem.exists(f("/codebase/foo"))).andReturn(true);
    expect(fileSystem.exists(f("/writer/foo"))).andReturn(true);

    expect(fileSystem.isExecutable(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.isExecutable(f("/writer/foo"))).andReturn(false);
    fileSystem.makeDirsForFile(f("/writer/foo"));
    fileSystem.copyFile(f("/codebase/foo"), f("/writer/foo"));
    control.replay();
    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"));
    e.putFile("foo", c);
    control.verify();
  }

  public void testNewFile() throws Exception {
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
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"));
    e.putFile("foo", c);
    control.verify();
  }

  public void testNewHtmlFile() throws Exception {
    expect(fileSystem.exists(f("/codebase/test.html"))).andReturn(true);
    expect(fileSystem.exists(f("/writer/test.html"))).andReturn(false);

    expect(fileSystem.isExecutable(f("/codebase/test.html"))).andReturn(false);
    expect(fileSystem.isExecutable(f("/writer/test.html"))).andReturn(false);
    fileSystem.makeDirsForFile(f("/writer/test.html"));
    fileSystem.copyFile(f("/codebase/test.html"), f("/writer/test.html"));
    expectSvnCommand(ImmutableList.of("add", "--parents", "test.html"), "/writer", "", cmd);
    expectSvnCommand(ImmutableList.of("propset", "svn:mime-type", "text/html", "test.html"),
        "/writer", "", cmd);
    control.replay();

    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"));
    e.putFile("test.html", c);
    control.verify();
  }

  public void testSetExecutable() throws Exception {
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
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"));
    e.putFile("foo", c);
    control.verify();
  }

  public void testDeleteExecutable() throws Exception {
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
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"));
    e.putFile("foo", c);
    control.verify();
  }

  public void testPutEmptyCodebaseWithMetadata() throws Exception {
    expect(fileSystem.findFiles(f("/codebase"))).andReturn(ImmutableSet.<File>of());
    expect(fileSystem.findFiles(f("/writer"))).andReturn(
        ImmutableSet.<File>of(f("/writer/.svn/")));

    File script = new File("/writer/svn_commit.sh");
    fileSystem.write("#!/bin/sh -e\n" +
                     "svn update\n" +
                     "svn commit -m \"desc\"\n" +
                     "svn propset -r HEAD svn:author \"author\" --revprop",
                     script);
    fileSystem.setExecutable(script);

    control.replay();
    Codebase c = new Codebase(f("/codebase"), "public",
                              e("public", ImmutableMap.<String, String>of()));
    RevisionMetadata rm = new RevisionMetadata("rev1", "author", "data", "desc",
                                               ImmutableList.<Revision>of());
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"));
    DraftRevision r = e.putCodebase(c, rm);
    control.verify();
    assertEquals("/writer", r.getLocation());
  }
}
