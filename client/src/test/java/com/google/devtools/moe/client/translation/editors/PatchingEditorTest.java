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

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class PatchingEditorTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);


  public void testNoSuchPatchFile() throws Exception {
    File patcherRun = new File("/patcher_run_foo");
    File codebaseFile = new File("/codebase");
    Codebase codebase =
        Codebase.create(codebaseFile, "internal", new RepositoryExpression("ignored"));
    Map<String, String> options = new HashMap<>();
    options.put("file", "notFile");

    expect(fileSystem.getTemporaryDirectory("patcher_run_")).andReturn(patcherRun);
    expect(fileSystem.isReadable(new File("notFile"))).andReturn(false);

    control.replay();

    try {
      new PatchingEditor(cmd, fileSystem, "patcher", null).edit(codebase, options);
      fail();
    } catch (MoeProblem e) {
      assertEquals("cannot read file notFile", e.getMessage());
    }
    control.verify();
  }

  public void testPatching() throws Exception {
    File patcherRun = new File("/patcher_run_foo");
    File patchFile = new File("/patchfile");
    File codebaseFile = new File("/codebase");

    Codebase codebase =
        Codebase.create(codebaseFile, "internal", new RepositoryExpression("ignored"));

    Map<String, String> options = new HashMap<>();
    options.put("file", "/patchfile");

    expect(fileSystem.getTemporaryDirectory("patcher_run_")).andReturn(patcherRun);
    expect(fileSystem.isReadable(patchFile)).andReturn(true);
    fileSystem.copyDirectory(codebaseFile, patcherRun);

    expect(
            cmd.runCommand(
                "/patcher_run_foo", "patch", ImmutableList.of("-p0", "--input=/patchfile")))
        .andReturn("");

    control.replay();

    new PatchingEditor(cmd, fileSystem, "patcher", null).edit(codebase, options);

    control.verify();
  }
}
