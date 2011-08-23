// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Charsets;
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
  public File getTemporaryDirectory(String prefix) {
    File tempDir;
    try {
      tempDir = File.createTempFile(prefix, "");
      tempDir.delete();
    } catch (IOException e) {
      throw new MoeProblem("could not create temp file: " + e.getMessage());
    }
    return tempDir;
  }

  /**
   * Find files under a path.
   *
   */
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

  public File[] listFiles(File path) {
    return path.listFiles();
  }

  public boolean exists(File f) {
    return f.exists();
  }

  public String getName(File f) {
    return f.getName();
  }

  public boolean isFile(File f) {
    return f.isFile();
  }

  public boolean isDirectory(File f) {
    return f.isDirectory();
  }

  public boolean isExecutable(File f) {
    return exists(f) && f.canExecute();
  }

  public boolean isReadable(File f) {
    return exists(f) && f.canRead();
  }

  public void setExecutable(File f) {
    f.setExecutable(true);
  }

  public void makeDirsForFile(File f) throws IOException {
    Files.createParentDirs(f);
  }

  public void makeDirs(File f) throws IOException {
    Files.createParentDirs(new File(f, "foo"));
  }

  public void copyFile(File src, File dest) throws IOException {
    Files.copy(src, dest);
  }

  public void write(String contents, File f) throws IOException {
    Files.write(contents, f, Charsets.UTF_8);
  }

  public void deleteRecursively(File file) throws IOException {
    Files.deleteRecursively(file);
  }

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

  public String fileToString(File f) throws IOException {
      return Files.toString(f, Charsets.UTF_8);
  }
}
