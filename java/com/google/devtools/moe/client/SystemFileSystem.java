// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Preconditions;
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
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A {@link FileSystem} using the real local filesystem via operations in {@link File}.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
@Singleton
public class SystemFileSystem implements FileSystem {
  private final Map<File, Lifetime> tempDirLifetimes = Maps.newHashMap();

  @Inject
  public SystemFileSystem() {}

  @Override
  public File getTemporaryDirectory(String prefix) {
    return getTemporaryDirectory(prefix, Lifetimes.currentTask());
  }

  @Override
  public File getTemporaryDirectory(String prefix, Lifetime lifetime) {
    File tempDir;
    try {
      tempDir = File.createTempFile("moe_" + prefix, "");
      tempDir.delete();
    } catch (IOException e) {
      throw new MoeProblem("could not create temp file: " + e.getMessage());
    }
    tempDirLifetimes.put(tempDir, lifetime);
    return tempDir;
  }

  @Override
  public void cleanUpTempDirs() throws IOException {
    Iterator<Entry<File, Lifetime>> tempDirIterator = tempDirLifetimes.entrySet().iterator();
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

    for (File subFile : f.listFiles()) {
      findFilesRecursiveHelper(subFile, result);
    }
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
    Path directory = Paths.get(file.toURI());
    java.nio.file.Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        java.nio.file.Files.delete(file);
        return FileVisitResult.CONTINUE;
      }
      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        java.nio.file.Files.delete(dir);
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
  public String fileToString(File f) throws IOException {
    return Files.toString(f, UTF_8);
  }

  /** A Dagger module for binding this implementation of {@link FileSystem}. */
  @dagger.Module
  public static class Module {
    @Provides
    public FileSystem fileSystem(SystemFileSystem impl) {
      return impl;
    }
  }
}
