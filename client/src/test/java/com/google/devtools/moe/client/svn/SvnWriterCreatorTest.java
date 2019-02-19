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
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.TestingModule;
import dagger.Provides;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class SvnWriterCreatorTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final SvnUtil util = new SvnUtil(cmd);
  @Inject Ui ui;

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, Module.class})
  @Singleton
  interface Component {
    void inject(SvnWriterCreatorTest instance);
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
    Component c = DaggerSvnWriterCreatorTest_Component.builder().module(new Module()).build();
    c.inject(this);
  }

  public void testCreate() throws Exception {
    SvnRevisionHistory revisionHistory = control.createMock(SvnRevisionHistory.class);
    RepositoryConfig mockConfig = control.createMock(RepositoryConfig.class);
    expect(mockConfig.getUrl()).andReturn("http://foo/svn/trunk/").anyTimes();
    expect(mockConfig.getProjectSpace()).andReturn("internal").anyTimes();
    expect(mockConfig.getIgnoreFilePatterns()).andReturn(ImmutableList.<String>of()).anyTimes();

    Revision result = Revision.create(45, "");
    expect(fileSystem.getTemporaryDirectory("svn_writer_45_"))
        .andReturn(new File("/dummy/path/45"));
    expect(revisionHistory.findHighestRevision("45")).andReturn(result);
    expect(
            cmd.runCommand(
                "",
                "svn",
                ImmutableList.of(
                    "--no-auth-cache",
                    "co",
                    "-r",
                    "45",
                    "http://foo/svn/trunk/",
                    "/dummy/path/45")))
        .andReturn("");

    control.replay();
    SvnWriterCreator c = new SvnWriterCreator(mockConfig, revisionHistory, util, fileSystem, ui);
    c.create(ImmutableMap.of("revision", "45"));
    control.verify();

    try {
      c.create(ImmutableMap.of("revisionmisspelled", "45"));
      fail();
    } catch (MoeProblem expected) {}
  }
}
