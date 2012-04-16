// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

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

  public void testListFiles() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    File bar = new File(tempDir, "bar");
    Files.touch(foo);
    Files.touch(bar);
    ImmutableSet<File> files = ImmutableSet.copyOf(fs.listFiles(tempDir));
    assertEquals(true, files.contains(foo));
    assertEquals(true, files.contains(bar));
  }

  public void testExists() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    assertEquals(true, fs.exists(foo));
  }

  public void testGetName() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    assertEquals(foo.getName(), fs.getName(foo));
  }

  public void testIsFile() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    assertEquals(true, fs.isFile(foo));
    assertEquals(false, fs.isFile(tempDir));
  }

  public void testIsDirectory() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    assertEquals(true, fs.isDirectory(tempDir));
    assertEquals(false, fs.isDirectory(foo));
  }

  public void testExecutable() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    foo.setExecutable(true);
    assertEquals(true, fs.isExecutable(foo));
  }

  public void testReadable() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    foo.setReadable(true);
    assertEquals(true, fs.isReadable(foo));
  }

  public void testSetExecutable() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.touch(foo);
    fs.setExecutable(foo);
    assertEquals(true, foo.canExecute());
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

  public void testWrite() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    fs.write("Contents!", foo);
    assertEquals("Contents!", Files.toString(foo, Charsets.UTF_8));
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

  public void testFileToString() throws Exception {
    FileSystem fs = new SystemFileSystem();
    File tempDir = Files.createTempDir();
    File foo = new File(tempDir, "foo");
    Files.write("Contents!", foo, Charsets.UTF_8);
    assertEquals("Contents!", fs.fileToString(foo));
  }

  private File touchTempDir(String prefix, FileSystem fs) throws IOException {
    File out = fs.getTemporaryDirectory(prefix, Lifetimes.currentTask());
    Files.touch(out);
    return out;
  }

  public void testCleanUpTempDirsWithTasks() throws Exception {
    AppContextForTesting.initForTest();
    FileSystem fs = new SystemFileSystem();
    AppContext.RUN.fileSystem = fs;

    File taskless = fs.getTemporaryDirectory("taskless", Lifetimes.moeExecution());
    Files.touch(taskless);

    Task outer = AppContext.RUN.ui.pushTask("outer", "outer");
    File outer1 = touchTempDir("outer1", fs);
    File outer2 = touchTempDir("outer2", fs);

    Task inner = AppContext.RUN.ui.pushTask("inner", "inner");
    File inner1 = touchTempDir("inner1", fs);
    File inner2 = touchTempDir("inner2", fs);
    File innerPersist = fs.getTemporaryDirectory("innerPersist", Lifetimes.moeExecution());
    Files.touch(innerPersist);

    AppContext.RUN.ui.popTask(inner, "popping inner, persisting nothing");
    assertFalse("inner1", inner1.exists());
    assertFalse("inner2", inner2.exists());
    assertTrue("innerPersist", innerPersist.exists());
    assertTrue("taskless", taskless.exists());
    assertTrue("outer1", outer1.exists());
    assertTrue("outer2", outer2.exists());

    AppContext.RUN.ui.popTask(outer, "popping outer, persisting nothing");
    assertFalse("outer1", outer1.exists());
    assertFalse("outer2", outer2.exists());
    assertTrue("innerPersist", innerPersist.exists());
    assertTrue("taskless", taskless.exists());

    Task moeTermination =
        AppContext.RUN.ui.pushTask(Moe.MOE_TERMINATION_TASK_NAME, "Final clean-up");
    fs.cleanUpTempDirs();
    AppContext.RUN.ui.popTask(moeTermination, "Finished clean-up");
    assertFalse("innerPersist", innerPersist.exists());
    assertFalse("taskless", taskless.exists());
  }

  public void testMarkAsPersistentWithTasks() throws Exception {
    AppContextForTesting.initForTest();
    FileSystem fs = new SystemFileSystem();
    AppContext.RUN.fileSystem = fs;

    Task outer = AppContext.RUN.ui.pushTask("outer", "outer");
    File outer1 = touchTempDir("outer1", fs);
    File outer2 = touchTempDir("outer2", fs);

    Task inner = AppContext.RUN.ui.pushTask("inner", "inner");
    File inner1 = touchTempDir("inner1", fs);
    File inner2 = touchTempDir("inner2", fs);

    AppContext.RUN.ui.popTaskAndPersist(inner, inner1);
    assertTrue("inner1", inner1.exists());
    assertFalse("inner2", inner2.exists());
    assertTrue("outer1", outer1.exists());
    assertTrue("outer2", outer2.exists());

    AppContext.RUN.ui.popTaskAndPersist(outer, outer1);
    assertFalse("inner1", inner1.exists());
    assertTrue("outer1", outer1.exists());
    assertFalse("outer2", outer2.exists());

    Task moeTermination =
        AppContext.RUN.ui.pushTask(Moe.MOE_TERMINATION_TASK_NAME, "Final clean-up");
    fs.cleanUpTempDirs();
    AppContext.RUN.ui.popTask(moeTermination, "Finished clean-up");
    // outer1 was persisted from a top-level task, so it shouldn't be cleaned up at all.
    assertTrue("outer1", outer1.exists());
  }
}
