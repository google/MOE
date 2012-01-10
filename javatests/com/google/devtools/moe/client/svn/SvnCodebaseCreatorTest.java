// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.project.RepositoryConfig;
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
public class SvnCodebaseCreatorTest extends TestCase {

  public void testExportExplicitRevision() throws Exception {
    AppContextForTesting.initForTest();
    Revision result = new Revision("45", "");
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

    expect(revisionHistory.findHighestRevision("46")).andReturn(result);
    expect(fileSystem.getTemporaryDirectory("svn_export_testing_45_")).
        andReturn(new File("/dummy/path/45"));
    expect(cmd.runCommand(
        "svn",
        ImmutableList.of("--no-auth-cache", "export", "http://foo/svn/trunk/", "-r", "45",
                         "/dummy/path/45"), "")).andReturn("");
    // Short-circuit Utils.filterFiles for ignore_files_re.
    expect(AppContext.RUN.fileSystem.findFiles(new File("/dummy/path/45")))
        .andReturn(ImmutableSet.<File>of());

    control.replay();
    CodebaseCreator cc = new SvnCodebaseCreator("testing", mockConfig, revisionHistory);
    Codebase r = cc.create(ImmutableMap.of("revision", "46"));
    assertEquals("/dummy/path/45", r.getPath().getAbsolutePath());
    assertEquals("internal", r.getProjectSpace());
    control.verify();
  }
}
