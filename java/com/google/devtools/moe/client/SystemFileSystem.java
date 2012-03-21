// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SystemFileSystem implements FileSystem {

  /**
   * Directories to delete on cleanUpTempDirs().
   */
  private final Set<File> tempDirs = Sets.newHashSet();

  @Override
  public File getTemporaryDirectory(String prefix) {
    File tempDir;
    try {
      tempDir = File.createTempFile("moe_" + prefix, "");
      tempDir.delete();
    } catch (IOException e) {
      throw new MoeProblem("could not create temp file: " + e.getMessage());
    }
    tempDirs.add(tempDir);
    return tempDir;
  }

  @Override
  public void cleanUpTempDirs() throws IOException {
    for (File tempDir : tempDirs) {
      deleteRecursively(tempDir);
      if (AppContext.RUN != null) {
        AppContext.RUN.ui.debug("Deleted temp dir: " + tempDir);
      }
    }
    tempDirs.clear();
  }

  @Override
  public void markAsPersistent(File path) {
    Preconditions.checkState(tempDirs.contains(path), "persisting unknown path");
    tempDirs.remove(path);
  }

  /**
   * Find files under a path.
   *
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
    f.setExecutable(true);
  }

  @Override
  public void setNonExecutable(File f) {
    f.setExecutable(false);
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
    dest.setExecutable(src.canExecute());
  }

  @Override
  public void write(String contents, File f) throws IOException {
    Files.write(contents, f, Charsets.UTF_8);
  }

  @Override
  public void deleteRecursively(File file) throws IOException {
    Files.deleteRecursively(file);
  }

  @Override
  public File getResourceAsFile(String resource) throws IOException {
    String name = (new File(resource)).getName();
    if (name.isEmpty()) {
      throw new IOException("Invalid resource name: " + resource);
    }

    File extractedFile = new File(
        AppContext.RUN.fileSystem.getTemporaryDirectory("resource_extraction_"),
        name);
    AppContext.RUN.fileSystem.makeDirsForFile(extractedFile);
    OutputStream os = Files.newOutputStreamSupplier(extractedFile).getOutput();
    Resources.copy(
        SystemFileSystem.class.getResource(resource), os);
    os.close();
    return extractedFile;
  }

  @Override
  public String fileToString(File f) throws IOException {
      return Files.toString(f, Charsets.UTF_8);
  }
}
