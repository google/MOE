// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Utils;

import junit.framework.TestCase;
import java.io.File;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SystemFileSystemTest extends TestCase {

  public void testFindFiles() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    File baz = new File(tempDir, "bar/baz");
    Files.createParentDirs(baz);
    Files.touch(baz);

    assertEquals(ImmutableSet.of("foo", "bar/baz"),
                 Utils.makeFilenamesRelative(fs.findFiles(tempDir), tempDir));
  }

  public void testExists() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    assertEquals(true, fs.exists(foo));
  }

  public void testExecutable() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    foo.setExecutable(true);
    assertEquals(true, fs.isExecutable(foo));
  }

  public void testMakeDirsForFile() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File baz = new File(tempDir, "foo/bar/baz");
    File bar = new File(tempDir, "foo/bar");
    fs.makeDirsForFile(baz);
    assertEquals(true, bar.exists());
    assertEquals(true, bar.isDirectory());
    assertEquals(false, baz.exists());
  }

  public void testMakeDirs() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File baz = new File(tempDir, "foo/bar/baz");
    File bar = new File(tempDir, "foo/bar");
    fs.makeDirs(baz);
    assertEquals(true, bar.exists());
    assertEquals(true, bar.isDirectory());
    assertEquals(true, baz.exists());
    assertEquals(true, baz.isDirectory());
  }

  public void testCopy() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    File bar = new File(tempDir, "bar");
    Files.write("Contents!", foo, Charsets.UTF_8);
    fs.copyFile(foo, bar);
    assertEquals(true, Files.equal(foo, bar));
  }

  public void testDeleteRecursively() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    File bar = new File(tempDir, "bar");
    Files.write("Contents!", foo, Charsets.UTF_8);
    fs.copyFile(foo, bar);
    fs.deleteRecursively(tempDir);
    assertEquals(false, tempDir.exists());
    assertEquals(false, foo.exists());
    assertEquals(false, bar.exists());
  }


}
