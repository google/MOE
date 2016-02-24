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

import static java.nio.file.Files.walkFileTree;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import dagger.Provides;
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

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A {@link FileSystem} using the real local filesystem via operations in {@link File}.
 */
@Singleton
public class SystemFileSystem implements FileSystem {
  private final Map<File, Lifetime> temporaryDirectoryOfLifetimes = Maps.newHashMap();

  @Inject
  public SystemFileSystem() {}

  @Override
  public File getTemporaryDirectory(String prefix) {
    return getTemporaryDirectory(prefix, Lifetimes.currentTask());
  }

  @Override
  public File getTemporaryDirectory(String prefix, Lifetime lifetime) {
    File temporaryDirectory;
    try {
      temporaryDirectory = File.createTempFile("moe_" + prefix, "");
      temporaryDirectory.delete();
    } catch (IOException e) {
      throw new MoeProblem("could not create temp file: " + e.getMessage());
    }
    temporaryDirectoryOfLifetimes.put(temporaryDirectory, lifetime);
    return temporaryDirectory;
  }

  @Override
  public void cleanUpTempDirs() throws IOException {
    Iterator<Entry<File, Lifetime>> tempDirIterator = temporaryDirectoryOfLifetimes.entrySet().iterator();
    while (tempDirIterator.hasNext()) {
      Entry<File, Lifetime> entry = tempDirIterator.next();
      if (entry.getValue().shouldCleanUp()) {
        deleteRecursively(entry.getKey());
        tempDirIterator.remove();
      }
    }
  }

  @Override
  public void setLifetime(File path, Lifetime lifetime) {
    Preconditions.checkState(
        temporaryDirectoryOfLifetimes.containsKey(path),
        "Trying to set the Lifetime for an unknown path: %s",
        path);
    temporaryDirectoryOfLifetimes.put(path, lifetime);
  }

  @Override
  public Set<File> findFiles(File path) {
    Set<File> result = Sets.newHashSet();
    findFilesRecursiveHelper(path, result);
    return result;
  }

  /**
   * Finds a file recursively in a directory.
   * 
   * @param file file to be searched.
   * @param result set where the found files will be added.
   */
  void findFilesRecursiveHelper(File file, Set<File> result) {
    if (file.exists() && file.isFile()) {
      result.add(file);
      return;
    }

    for (File subFile : file.listFiles()) {
      findFilesRecursiveHelper(subFile, result);
    }
  }

  @Override
  public File[] listFiles(File path) {
    return path.listFiles();
  }

  @Override
  public boolean exists(File file) {
    return file.exists();
  }

  @Override
  public String getName(File file) {
    return file.getName();
  }

  @Override
  public boolean isFile(File file) {
    return file.isFile();
  }

  @Override
  public boolean isDirectory(File file) {
    return file.isDirectory();
  }

  @Override
  public boolean isExecutable(File file) {
    return exists(file) && file.canExecute();
  }

  @Override
  public boolean isReadable(File file) {
    return exists(file) && file.canRead();
  }

  @Override
  public void setExecutable(File file) {
    file.setExecutable(true, false);
  }

  @Override
  public void setNonExecutable(File file) {
    file.setExecutable(false, false);
  }

  @Override
  public void makeDirsForFile(File file) throws IOException {
    Files.createParentDirs(file);
  }

  @Override
  public void makeDirs(File file) throws IOException {
    Files.createParentDirs(new File(file, "foo"));
  }

  @Override
  public void copyFile(File source, File destination) throws IOException {
    Files.copy(source, destination);
    destination.setExecutable(source.canExecute(), false);
  }

  @Override
  public void write(String contents, File file) throws IOException {
    Files.write(contents, file, UTF_8);
  }

  @Override
  public void deleteRecursively(File file) throws IOException {
    deleteRecursively(file.toPath());
  }

  /**
   * Deletes a file recursively.
   * Note, this does not attempt to perform the action securely and is 
   * vulnerable to a racey replacement of a directory about to be deleted with 
   * a symlink which can lead to files outside the parent directory to be deleted.
   * 
   * @param path path of the file to be deleted.
   * 
   * @throws IOException if some error occurs while deleting the file.
   */
  private void deleteRecursively(final Path path) throws IOException {
    final List<IOException> exceptions = new ArrayList<>();
    walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        try {
          java.nio.file.Files.delete(file);
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
        new File(getTemporaryDirectory("resource_extraction_", Lifetimes.moeExecution()), name);
    makeDirsForFile(extractedFile);
    OutputStream os = Files.asByteSink(extractedFile).openStream();
    Resources.copy(SystemFileSystem.class.getResource(resource), os);
    os.close();
    return extractedFile;
  }

  @Override
  public String fileToString(File file) throws IOException {
    return Files.toString(file, UTF_8);
  }

  /** 
   * A Dagger module for binding this implementation of {@link FileSystem}.
   */
  @dagger.Module
  public static class Module {
    @Provides
    public FileSystem fileSystem(SystemFileSystem impl) {
      return impl;
    }
  }
}
