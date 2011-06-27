// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.MoeProblem;

import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;
import junit.framework.TestCase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class PatchingEditorTest extends TestCase {
  public void testNoSuchPatchFile() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    AppContext.RUN.fileSystem = fileSystem;

    File patcherRun = new File("/patcher_run_foo");
    File codebaseFile = new File("/codebase");
    Map<String, String> context = new HashMap<String, String>();
    context.put("file", "notFile");

    expect(fileSystem.getTemporaryDirectory("patcher_run_")).andReturn(patcherRun);
    expect(fileSystem.isReadable(new File("notFile"))).andReturn(false);

    control.replay();

    try {
      new PatchingEditor("patcher").edit(codebaseFile, context);
      fail();
    } catch (MoeProblem e) {
      assertEquals("cannot read file notFile", e.getMessage());
    }
    control.verify();
  }

  public void testPatching() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File patcherRun = new File("/patcher_run_foo");
    File patchFile = new File("/patchfile");
    File codebaseFile = new File("/codebase");
    Map<String, String> options = new HashMap<String, String>();
    options.put("file", "/patchfile");

    expect(fileSystem.getTemporaryDirectory("patcher_run_")).andReturn(patcherRun);
    expect(fileSystem.isReadable(patchFile)).andReturn(true);
    fileSystem.makeDirsForFile(patcherRun);
    expect(fileSystem.isFile(codebaseFile)).andReturn(false);
    expect(fileSystem.listFiles(codebaseFile)).andReturn(new File[] {});

    expect(cmd.runCommand(
        "patch",
        ImmutableList.of("-p0",
                         "--input=/patchfile"),
        "", "/patcher_run_foo")).andReturn("");

    control.replay();

    new PatchingEditor("patcher").edit(codebaseFile, options);
    control.verify();

  }
}
