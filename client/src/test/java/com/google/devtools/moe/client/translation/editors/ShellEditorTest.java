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

package com.google.devtools.moe.client.translation.editors;

import static com.google.devtools.moe.client.config.EditorType.shell;
import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.GsonModule;
import com.google.devtools.moe.client.config.EditorConfig;
import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class ShellEditorTest extends TestCase {
  private static final String CMD = "touch test.txt";
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);

  public void testShellStuff() throws Exception {
    File shellRun = new File("/shell_run_foo");
    File codebaseFile = new File("/codebase");

    Codebase codebase =
        Codebase.create(codebaseFile, "internal", new RepositoryExpression("ignored"));

    expect(fileSystem.getTemporaryDirectory("shell_run_")).andReturn(shellRun);
    fileSystem.copyDirectory(codebaseFile, shellRun);

    List<String> argsList = new ArrayList<>();
    argsList.add("-c");
    argsList.add("touch test.txt");

    expect(cmd.runCommand("/shell_run_foo", "bash", argsList)).andReturn("");

    control.replay();

    ScrubberConfig scrubberConfig = GsonModule.provideGson().fromJson("{}", ScrubberConfig.class);
    EditorConfig config = EditorConfig.create(shell, scrubberConfig, CMD, new JsonObject(), false);
    new ShellEditor(cmd, fileSystem, "shell_editor", config)
        .edit(codebase, ImmutableMap.<String, String>of());

    control.verify();
  }
}
