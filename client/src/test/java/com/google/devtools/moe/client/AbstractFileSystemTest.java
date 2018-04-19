/*
 * Copyright (c) 2016 Google, Inc.
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

package com.google.devtools.moe.client;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.testing.InMemoryFileSystem;
import java.io.File;
import junit.framework.TestCase;

public class AbstractFileSystemTest extends TestCase {
  private final FileSystem fileSystem =
      new InMemoryFileSystem(ImmutableMap.of("/src/dummy/file", "contents"), null);

  public void testCopyDirectory() throws Exception {
    File src = new File("/src");
    File dest = new File("/dest");

    fileSystem.copyDirectory(src, dest);

    assertThat(fileSystem.isDirectory(dest)).named("isDirectory(/dest)").isTrue();
    assertThat(fileSystem.isDirectory(new File("/dest/dummy")))
        .named("isDirectory(/dest/dummy)")
        .isTrue();
    assertThat(fileSystem.exists(new File("/dest/dummy/file"))).named("exists()").isTrue();
  }
}
