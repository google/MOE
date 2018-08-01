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

package com.google.devtools.moe.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.walkFileTree;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.devtools.moe.client.qualifiers.Flag;
import dagger.Binds;
import dagger.Lazy;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/** A {@link FileSystem} using the real local filesystem via operations in {@link File}. */
@Singleton
public class SystemFileSystem extends AbstractFileSystem {
  private final Map<File, Lifetime> tempDirLifetimes = Maps.newHashMap();
  @Inject Lazy<Lifetimes> lifetimes;

  @Inject
  @Flag("debug")
  Lazy<Boolean> debug = () -> false;

  @Inject
  public SystemFileSystem() {}

  @Override
  public File getTemporaryDirectory(String prefix) {
    return getTemporaryDirectory(prefix, lifetimes.get().currentTask());
  }

  @Override
  public File getTemporaryDirectory(String prefix, Lifetime lifetime) {
    File tempDir;
    try {
      tempDir = File.createTempFile("moe_" + prefix, "");
      tempDir.delete();
    } catch (IOException e) {
      throw new MoeProblem(e, "could not create temp file");
    }
    tempDirLifetimes.put(tempDir, lifetime);
    return tempDir;
  }

  @Override
  public void cleanUpTempDirs() throws IOException {
    Iterator<Entry<File, Lifetime>> tempDirIterator = tempDirLifetimes.entrySet().iterator();
    if (!debug.get()) {
      while (tempDirIterator.hasNext()) {
        Entry<File, Lifetime> entry = tempDirIterator.next();
        if (entry.getValue().shouldCleanUp()) {
          deleteRecursively(entry.getKey());
          tempDirIterator.remove();
        }
      }
    }
  }

  @Override
  public void setLifetime(File path, Lifetime lifetime) {
    Preconditions.checkState(
        tempDirLifetimes.containsKey(path),
        "Trying to set the Lifetime for an unknown path: %s",
        path);
    tempDirLifetimes.put(path, lifetime);
  }

  /**
   * Find files under a path.
   */
  @Override
  public Set<File> findFiles(File path) {
    Set<File> result = Sets.newHashSet();
    findFilesRecursiveHelper(path, result);
    return result;
  }

  void findFilesRecursiveHelper(File f, Set<File> result) {
    if (f.exists() && f.isFile()) {
      result.add(f);
      return;
    }

    for (File subFile : nullSafeArray(f.listFiles())) {
      findFilesRecursiveHelper(subFile, result);
    }
  }

  private File[] nullSafeArray(File[] array) {
    return array == null ? new File[0] : array;
  }

  @Override
  public File[] listFiles(File path) {
    return path.listFiles();
  }

  @Override
  public boolean exists(File f) {
    return f.exists();
  }

  @Override
  public String getName(File f) {
    return f.getName();
  }

  @Override
  public boolean isFile(File f) {
    return f.isFile();
  }

  @Override
  public boolean isDirectory(File f) {
    return f.isDirectory();
  }

  @Override
  public boolean isExecutable(File f) {
    return exists(f) && f.canExecute();
  }

  @Override
  public boolean isReadable(File f) {
    return exists(f) && f.canRead();
  }

  @Override
  public void setExecutable(File f) {
    f.setExecutable(true, false);
  }

  @Override
  public void setNonExecutable(File f) {
    f.setExecutable(false, false);
  }

  @Override
  public void makeDirsForFile(File f) throws IOException {
    Files.createParentDirs(f);
  }

  @Override
  public void makeDirs(File f) throws IOException {
    Files.createParentDirs(new File(f, "foo"));
  }

  @Override
  public void copyFile(File src, File dest) throws IOException {
    Files.copy(src, dest);
    dest.setExecutable(src.canExecute(), false);
  }

  @Override
  public void write(String contents, File f) throws IOException {
    Files.write(contents, f, UTF_8);
  }

  @Override
  public void deleteRecursively(File file) throws IOException {
    deleteRecursively(file.toPath());
  }

  private void deleteRecursively(final Path path) throws IOException {
    // Note, this does not attempt to perform the action securely and is vulnerable to a
    // racey replacement of a directory about to be deleted with a symlink which can lead to
    // files outside the parent directory to be deleted.
    final List<IOException> exceptions = new ArrayList<>();
    walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        try {
          java.nio.file.Files.deleteIfExists(file);
        } catch (IOException e) {
          exceptions.add(e);
        }
        return FileVisitResult.CONTINUE;
      }
      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException ignore) throws IOException {
        // Since we're collecting exceptions in visitFile and never throwing, ignore the exception.
        try {
          java.nio.file.Files.delete(dir);
        } catch (IOException e) {
          exceptions.add(e);
          switch (exceptions.size()) {
            case 1:
              throw Iterables.getOnlyElement(exceptions);
            default:
              IOException wrapper = new IOException("Errors recursively deleting " + path);
              for (IOException exception : exceptions) {
                wrapper.addSuppressed(exception);
              }
              throw wrapper;
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  @Override
  public File getResourceAsFile(String resource) throws IOException {
    String name = (new File(resource)).getName();
    if (name.isEmpty()) {
      throw new IOException("Invalid resource name: " + resource);
    }

    File extractedFile =
        new File(
            getTemporaryDirectory("resource_extraction_", lifetimes.get().moeExecution()), name);
    makeDirsForFile(extractedFile);
    OutputStream os = Files.asByteSink(extractedFile).openStream();
    Resources.copy(SystemFileSystem.class.getResource(resource), os);
    os.close();
    return extractedFile;
  }

  @Override
  public String fileToString(File f) throws IOException {
    return Files.toString(f, UTF_8);
  }

  /** A Dagger module for binding this implementation of {@link FileSystem}. */
  @dagger.Module
  public abstract static class Module {
    @Binds
    public abstract FileSystem fileSystem(SystemFileSystem impl);
  }
}
