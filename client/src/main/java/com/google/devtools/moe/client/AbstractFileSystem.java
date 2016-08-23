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

import java.io.File;
import java.io.IOException;

/** A partial implementation of FileSystem */
public abstract class AbstractFileSystem implements FileSystem {
  @Override
  public void copyDirectory(File src, File dest) throws IOException {
    if (src == null) {
      return; // TODO(cgruber): Should this be an error?
    }
    this.makeDirsForFile(dest);
    if (this.isFile(src)) {
      this.copyFile(src, dest);
      return;
    }
    File[] files = this.listFiles(src);
    if (files == null) {
      return; // src did not represent a file or an io error occurred, per File.listFiles()
    }
    for (File subFile : files) {
      File newFile = new File(dest, this.getName(subFile));
      if (this.isDirectory(subFile)) {
        this.copyDirectory(subFile, newFile);
      } else {
        this.makeDirsForFile(newFile);
        this.copyFile(subFile, newFile);
      }
    }
  }
}
