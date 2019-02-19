/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.svn;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.TestingModule;
import com.google.devtools.moe.client.writer.DraftRevision;
import dagger.Provides;
import java.io.File;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.joda.time.DateTime;

public class SvnWriterTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final RepositoryConfig mockConfig = control.createMock(RepositoryConfig.class);
  private final SvnUtil util = new SvnUtil(cmd);
  @Inject Ui ui;

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, Module.class})
  @Singleton
  interface Component {
    void inject(SvnWriterTest instance);
  }

  @dagger.Module
  class Module {
    @Provides
    public CommandRunner commandRunner() {
      return cmd;
    }

    @Provides
    public FileSystem fileSystem() {
      return fileSystem;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Component c = DaggerSvnWriterTest_Component.builder().module(new Module()).build();
    c.inject(this);

    expect(mockConfig.getUrl()).andReturn("http://foo/svn/trunk/").anyTimes();
    expect(mockConfig.getProjectSpace()).andReturn("public").anyTimes();
    expect(mockConfig.getIgnoreFilePatterns()).andReturn(ImmutableList.<String>of()).anyTimes();
  }

  private void expectSvnCommand(
      List<String> args, String workingDirectory, String result, CommandRunner cmd)
      throws CommandException {
    ImmutableList.Builder<String> withAuthArgs = new ImmutableList.Builder<>();
    withAuthArgs.add("--no-auth-cache").addAll(args);
    expect(cmd.runCommand(workingDirectory, "svn", withAuthArgs.build())).andReturn(result);
  }

  private File f(String filename) {
    return new File(filename);
  }

  private RepositoryExpression e(
      String creatorIdentifier, ImmutableMap<String, String> creatorOptions) {
    return new RepositoryExpression(creatorIdentifier).withOptions(creatorOptions);
  }

  public void testPutEmptyCodebase() throws Exception {
    expect(fileSystem.findFiles(f("/codebase"))).andReturn(ImmutableSet.<File>of());
    expect(fileSystem.findFiles(f("/writer"))).andReturn(ImmutableSet.<File>of(f("/writer/.svn/")));

    control.replay();
    Codebase c =
        Codebase.create(f("/codebase"), "public", e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"), null, fileSystem, ui);
    DraftRevision r = e.putCodebase(c, null);
    control.verify();
    assertEquals("/writer", r.getLocation());
  }

  public void testWrongProjectSpace() throws Exception {
    Codebase c =
        Codebase.create(
            f("/codebase"), "internal", e("internal", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"), null, fileSystem, ui);
    try {
      e.putCodebase(c, null);
      fail();
    } catch (MoeProblem expected) {
    }
  }

  public void testDeletedFile() throws Exception {
    expect(fileSystem.exists(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.exists(f("/writer/foo"))).andReturn(true);

    expect(fileSystem.isExecutable(f("/codebase/foo"))).andReturn(false);
    expect(fileSystem.isExecutable(f("/writer/foo"))).andReturn(false);
    expectSvnCommand(ImmutableList.of("rm", "foo"), "/writer", "", cmd);
    control.replay();
    Codebase c =
        Codebase.create(f("/codebase"), "public", e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"), util, fileSystem, ui);
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
    Codebase c =
        Codebase.create(f("/codebase"), "public", e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"), null, fileSystem, ui);
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
    Codebase c =
        Codebase.create(f("/codebase"), "public", e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"), util, fileSystem, ui);
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
    expectSvnCommand(
        ImmutableList.of("propset", "svn:mime-type", "text/html", "test.html"), "/writer", "", cmd);
    control.replay();

    Codebase c =
        Codebase.create(f("/codebase"), "public", e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"), util, fileSystem, ui);
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
    Codebase c =
        Codebase.create(f("/codebase"), "public", e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"), util, fileSystem, ui);
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
    Codebase c =
        Codebase.create(f("/codebase"), "public", e("public", ImmutableMap.<String, String>of()));
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"), util, fileSystem, ui);
    e.putFile("foo", c);
    control.verify();
  }

  public void testPutEmptyCodebaseWithMetadata() throws Exception {
    expect(fileSystem.findFiles(f("/codebase"))).andReturn(ImmutableSet.<File>of());
    expect(fileSystem.findFiles(f("/writer"))).andReturn(ImmutableSet.<File>of(f("/writer/.svn/")));

    File script = new File("/writer/svn_commit.sh");
    fileSystem.write(
        "#!/bin/sh -e\n"
            + "svn update\n"
            + "svn commit -m \"desc\"\n"
            + "svn propset -r HEAD svn:author \"author\" --revprop",
        script);
    fileSystem.setExecutable(script);

    control.replay();
    Codebase c =
        Codebase.create(f("/codebase"), "public", e("public", ImmutableMap.<String, String>of()));
    RevisionMetadata rm =
        RevisionMetadata.builder()
            .id("rev1")
            .author("author")
            .date(new DateTime(1L))
            .description("desc")
            .build();
    SvnWriter e = new SvnWriter(mockConfig, null, f("/writer"), null, fileSystem, ui);
    DraftRevision r = e.putCodebase(c, rm);
    control.verify();
    assertEquals("/writer", r.getLocation());
  }
}
