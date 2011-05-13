// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;
import java.io.File;
import junit.framework.TestCase;

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

    Revision result = new Revision("45", "");
    expect(fileSystem.getTemporaryDirectory("svn_writer_45_")).
        andReturn(new File("/dummy/path/45"));
    expect(revisionHistory.findHighestRevision("45")).andReturn(result);
    expect(cmd.runCommand(
        "svn",
        ImmutableList.of("--no-auth-cache", "co", "-r", "45", "http://foo/svn/trunk/",
                         "/dummy/path/45"), "", "")).andReturn("");

    control.replay();
    SvnWriterCreator c = new SvnWriterCreator("public", "http://foo/svn/trunk/", "public",
                                              revisionHistory);
    c.create(ImmutableMap.of("revision", "45"));
    control.verify();

    try {
      c.create(ImmutableMap.of("revisionmisspelled", "45"));
      fail();
    } catch (MoeProblem p) {}
  }
}
