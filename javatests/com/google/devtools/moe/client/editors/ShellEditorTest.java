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

package com.google.devtools.moe.client.editors;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.testing.TestingModule;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

public class ShellEditorTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, Module.class})
  @Singleton
  interface Component {
    Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module
  class Module {
    @Provides
    public CommandRunner cmd() {
      return cmd;
    }

    @Provides
    public FileSystem filesystem() {
      return fileSystem;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE =
        DaggerShellEditorTest_Component.builder().module(new Module()).build().context();
  }

  public void testShellStuff() throws Exception {
    File shellRun = new File("/shell_run_foo");
    File codebaseFile = new File("/codebase");

    Codebase codebase =
        new Codebase(
            fileSystem,
            codebaseFile,
            "internal",
            null /* CodebaseExpression is not needed here. */);

    expect(fileSystem.getTemporaryDirectory("shell_run_")).andReturn(shellRun);
    fileSystem.makeDirsForFile(shellRun);
    expect(fileSystem.isFile(codebaseFile)).andReturn(false);
    expect(fileSystem.listFiles(codebaseFile)).andReturn(new File[] {});

    List<String> argsList = new ArrayList<>();
    argsList.add("-c");
    argsList.add("touch test.txt");

    expect(cmd.runCommand("bash", argsList, "/shell_run_foo")).andReturn("");

    control.replay();

    new ShellEditor("shell_editor", "touch test.txt")
        .edit(
            codebase,
            null /* this edit doesn't require a ProjectContext */,
            ImmutableMap.<String, String>of() /* this edit doesn't require options */);

    control.verify();
  }
}
