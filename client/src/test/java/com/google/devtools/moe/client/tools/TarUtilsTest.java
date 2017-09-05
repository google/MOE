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

package com.google.devtools.moe.client.tools;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import java.io.File;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class TarUtilsTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final TarUtils tarUtils = new TarUtils(fileSystem, cmd);

  public void testExpandTar() throws Exception {
    fileSystem.makeDirs(new File("/dummy/path/45.expanded"));
    expect(fileSystem.getTemporaryDirectory("expanded_tar_"))
        .andReturn(new File("/dummy/path/45.expanded"));
    expect(
            cmd.runCommand(
                "/dummy/path/45.expanded", "tar", ImmutableList.of("-xf", "/dummy/path/45.tar")))
        .andReturn("");
    control.replay();
    File expanded = tarUtils.expandTar(new File("/dummy/path/45.tar"));
    assertEquals(new File("/dummy/path/45.expanded"), expanded);
    control.verify();
  }
}
