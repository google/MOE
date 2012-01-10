// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnWriterCreatorTest extends TestCase {

  public void testCreate() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    SvnRevisionHistory revisionHistory = control.createMock(SvnRevisionHistory.class);
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

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
