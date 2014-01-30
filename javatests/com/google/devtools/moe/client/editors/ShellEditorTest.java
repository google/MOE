// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.testing.ExtendedTestModule;

import dagger.ObjectGraph;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.Vector;

/**
 *
 */
public class ShellEditorTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);

  @Override protected void setUp() throws Exception {
    super.setUp();
    ObjectGraph graph = ObjectGraph.create(new ExtendedTestModule(fileSystem, cmd));
    graph.injectStatics();
  }

  public void testShellStuff() throws Exception {
    File shellRun = new File("/shell_run_foo");
    File codebaseFile = new File("/codebase");

    Codebase codebase = new Codebase(codebaseFile,
                                     "internal",
                                     null /* CodebaseExpression is not needed here. */);


    expect(fileSystem.getTemporaryDirectory("shell_run_")).andReturn(shellRun);
    fileSystem.makeDirsForFile(shellRun);
    expect(fileSystem.isFile(codebaseFile)).andReturn(false);
    expect(fileSystem.listFiles(codebaseFile)).andReturn(new File[] {});

    Vector<String> argsList = new Vector<String>();
    argsList.add("-c");
    argsList.add("touch test.txt");

    expect(cmd.runCommand("bash", argsList, "/shell_run_foo")).andReturn("");

    control.replay();

    new ShellEditor("shell_editor", "touch test.txt")
        .edit(codebase,
              null /* this edit doesn't require a ProjectContext */,
              ImmutableMap.<String, String>of() /* this edit doesn't require options */);

    control.verify();
  }
}
