/*
 * Copyright (c) 2018 Google, Inc.
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
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/** No-op {@link FileSystem} that does nothing. */
@Singleton
public class NoopFileSystem implements FileSystem {
  @Inject
  public NoopFileSystem() {}

  @Override
  public File getTemporaryDirectory(String prefix) {
    return null;
  }

  @Override
  public File getTemporaryDirectory(String prefix, Lifetime lifetime) {
    return null;
  }

  @Override
  public void cleanUpTempDirs() throws IOException {}

  @Override
  public void setLifetime(File path, Lifetime lifetime) {}

  @Override
  public Set<File> findFiles(File path) {
    return null;
  }

  @Override
  public File[] listFiles(File path) {
    return null;
  }

  @Override
  public boolean exists(File f) {
    return false;
  }

  @Override
  public String getName(File f) {
    return null;
  }

  @Override
  public boolean isFile(File f) {
    return false;
  }

  @Override
  public boolean isDirectory(File f) {
    return false;
  }

  @Override
  public boolean isExecutable(File f) {
    return false;
  }

  @Override
  public boolean isReadable(File f) {
    return false;
  }

  @Override
  public void setExecutable(File f) {}

  @Override
  public void setNonExecutable(File f) {}

  @Override
  public void makeDirsForFile(File f) throws IOException {}

  @Override
  public void makeDirs(File f) throws IOException {}

  @Override
  public void copyFile(File src, File dest) throws IOException {}

  @Override
  public void copyDirectory(File src, File dest) throws IOException {}

  @Override
  public void write(String contents, File f) throws IOException {}

  @Override
  public void deleteRecursively(File file) throws IOException {}

  @Override
  public File getResourceAsFile(String resource) throws IOException {
    return null;
  }

  @Override
  public String fileToString(File f) throws IOException {
    return null;
  }
}
