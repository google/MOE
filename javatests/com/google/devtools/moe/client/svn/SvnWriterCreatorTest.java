// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Moe;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.TestingModule;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

import javax.inject.Singleton;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnWriterCreatorTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, Module.class})
  @Singleton
  interface Component extends Moe.Component {
    @Override Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module
  class Module {
    @Provides public CommandRunner commandRunner() {
      return cmd;
    }
    @Provides public FileSystem fileSystem() {
      return fileSystem;
    }
  }

  @Override protected void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = DaggerSvnWriterCreatorTest_Component.builder().module(new Module()).build()
        .context();
  }

  public void testCreate() throws Exception {
    SvnRevisionHistory revisionHistory = control.createMock(SvnRevisionHistory.class);
    RepositoryConfig mockConfig = control.createMock(RepositoryConfig.class);
    expect(mockConfig.getUrl()).andReturn("http://foo/svn/trunk/").anyTimes();
    expect(mockConfig.getProjectSpace()).andReturn("internal").anyTimes();
    expect(mockConfig.getIgnoreFileRes()).andReturn(ImmutableList.<String>of()).anyTimes();

    Revision result = new Revision("45", "");
    expect(fileSystem.getTemporaryDirectory("svn_writer_45_")).
        andReturn(new File("/dummy/path/45"));
    expect(revisionHistory.findHighestRevision("45")).andReturn(result);
    expect(cmd.runCommand(
        "svn",
        ImmutableList.of("--no-auth-cache", "co", "-r", "45", "http://foo/svn/trunk/",
                         "/dummy/path/45"), "")).andReturn("");

    control.replay();
    SvnWriterCreator c = new SvnWriterCreator(mockConfig, revisionHistory);
    c.create(ImmutableMap.of("revision", "45"));
    control.verify();

    try {
      c.create(ImmutableMap.of("revisionmisspelled", "45"));
      fail();
    } catch (MoeProblem p) {}
  }
}
