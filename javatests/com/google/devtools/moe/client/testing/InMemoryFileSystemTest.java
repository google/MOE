// Copyright 2012 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.Lifetimes;

import dagger.ObjectGraph;

import junit.framework.TestCase;

import java.io.File;

/**
 * Unit tests for {@link InMemoryFileSystem}.
 *
 */
public class InMemoryFileSystemTest extends TestCase {

  @Override protected void setUp() throws Exception {
    super.setUp();
    ObjectGraph graph = ObjectGraph.create(new ExtendedTestModule(null, null));
    graph.injectStatics();
  }

  private static final Lifetime TRANSIENT = new Lifetime() {
    @Override public boolean shouldCleanUp() {
      return true;
    }
  };

  // Class under test
  private final InMemoryFileSystem fs = new InMemoryFileSystem();

  public void testGetTemporaryDirectory() throws Exception {
    File tempDirPineapple = fs.getTemporaryDirectory("pineapple", Lifetimes.persistent());
    File tempDirBanana = fs.getTemporaryDirectory("banana", Lifetimes.persistent());

    assertTrue(tempDirPineapple.getAbsolutePath().contains("pineapple"));
    assertTrue(tempDirBanana.getAbsolutePath().contains("banana"));
    assertFalse(fs.exists(tempDirPineapple));
    assertFalse(fs.exists(tempDirBanana));
  }

  public void testCleanUpTempDirs() throws Exception {
    File persistentDir = fs.getTemporaryDirectory("persistent", Lifetimes.persistent());
    File transientDir = fs.getTemporaryDirectory("transient", TRANSIENT);

    fs.makeDirs(persistentDir);
    fs.makeDirs(transientDir);
    fs.cleanUpTempDirs();

    assertTrue(fs.exists(persistentDir) && fs.isDirectory(persistentDir));
    assertFalse(fs.exists(transientDir));
  }

  public void testSetLifetime() throws Exception {
    File persistentDir = fs.getTemporaryDirectory("persistent", Lifetimes.persistent());
    File transientDir = fs.getTemporaryDirectory("transient", TRANSIENT);

    fs.makeDirs(persistentDir);
    fs.makeDirs(transientDir);
    fs.setLifetime(transientDir, Lifetimes.persistent());
    fs.cleanUpTempDirs();

    assertTrue(fs.exists(persistentDir) && fs.isDirectory(persistentDir));
    assertTrue(fs.exists(transientDir) && fs.isDirectory(transientDir));
  }

  public void testFindFiles() throws Exception {
    InMemoryFileSystem fs = new InMemoryFileSystem(ImmutableMap.of(
        "/dir/1", "1 contents",
        "/dir/2", "2 contents",
        "/dir/subdir/1", "subdir/1 contents",
        "/otherdir/1", "/otherdir/1 contents"
        ));

    assertEquals(
        ImmutableSet.of(new File("/dir/1"), new File("/dir/2"), new File("/dir/subdir/1")),
        fs.findFiles(new File("/dir")));
  }

  public void testListFiles() throws Exception {
    InMemoryFileSystem fs = new InMemoryFileSystem(ImmutableMap.of(
        "/dir/1", "1 contents",
        "/dir/2", "2 contents",
        "/dir/subdir/1", "subdir/1 contents",
        "/otherdir/1", "/otherdir/1 contents"
        ));

    assertEquals(
        ImmutableSet.of(new File("/dir/1"), new File("/dir/2"), new File("/dir/subdir")),
        ImmutableSet.copyOf(fs.listFiles(new File("/dir"))));
  }

  public void testExists() throws Exception {
    InMemoryFileSystem fs = new InMemoryFileSystem(ImmutableMap.of(
        "/dir/1", "1 contents",
        "/dir/subdir/1", "subdir/1 contents",
        "/otherdir/1", "/otherdir/1 contents"
        ));

    assertTrue(fs.exists(new File("/dir")));
    assertTrue(fs.exists(new File("/dir/1")));
    assertTrue(fs.exists(new File("/dir/subdir")));
    assertTrue(fs.exists(new File("/dir/subdir/1")));
    assertTrue(fs.exists(new File("/otherdir")));
    assertTrue(fs.exists(new File("/otherdir/1")));
    assertFalse(fs.exists(new File("/nonexistent")));
    assertFalse(fs.exists(new File("/dir/nonexistent")));
  }

  public void testIsFile() throws Exception {
    InMemoryFileSystem fs = new InMemoryFileSystem(ImmutableMap.of(
        "/dir/1", "1 contents",
        "/dir/subdir/1", "subdir/1 contents"
        ));

    assertFalse(fs.isFile(new File("/dir")));
    assertTrue(fs.isFile(new File("/dir/1")));
    assertFalse(fs.isFile(new File("/dir/nonexistent")));
    assertFalse(fs.isFile(new File("/dir/subdir")));
    assertTrue(fs.isFile(new File("/dir/subdir/1")));
  }

  public void testIsDirectory() throws Exception {
    InMemoryFileSystem fs = new InMemoryFileSystem(ImmutableMap.of(
        "/dir/1", "1 contents",
        "/dir/subdir/1", "subdir/1 contents"
        ));

    assertTrue(fs.isDirectory(new File("/dir")));
    assertFalse(fs.isDirectory(new File("/dir/1")));
    assertFalse(fs.isDirectory(new File("/dir/nonexistent")));
    assertTrue(fs.isDirectory(new File("/dir/subdir")));
    assertFalse(fs.isDirectory(new File("/dir/subdir/1")));
  }

  public void testMakeDirsForFile() throws Exception {
    fs.makeDirsForFile(new File("/a/b/c/d"));

    assertTrue(fs.isDirectory(new File("/a")));
    assertTrue(fs.isDirectory(new File("/a/b")));
    assertTrue(fs.isDirectory(new File("/a/b/c")));
    assertFalse(fs.exists(new File("/a/b/c/d")));
  }

  public void testMakeDirs() throws Exception {
    fs.makeDirs(new File("/a/b/c/d"));

    assertTrue(fs.isDirectory(new File("/a")));
    assertTrue(fs.isDirectory(new File("/a/b")));
    assertTrue(fs.isDirectory(new File("/a/b/c")));
    assertTrue(fs.isDirectory(new File("/a/b/c/d")));
  }

  public void testCopyFile() throws Exception {
    InMemoryFileSystem fs = new InMemoryFileSystem(ImmutableMap.of("/src", "contents"));
    fs.copyFile(new File("/src"), new File("/dest"));
    assertEquals("contents", fs.fileToString(new File("/dest")));
  }

  public void testWrite() throws Exception {
    fs.write("contents", new File("/src"));
    assertEquals("contents", fs.fileToString(new File("/src")));
  }

  public void testGetResourceAsFile() throws Exception {
    File resourceLocation = fs.getResourceAsFile("test_resource");
    assertTrue(fs.isFile(resourceLocation));
  }

  public void testFileToString() throws Exception {
    InMemoryFileSystem fs = new InMemoryFileSystem(ImmutableMap.of("/file", "contents"));
    assertEquals("contents", fs.fileToString(new File("/file")));
  }
}
