// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.gson.JsonObject;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.Map;

/**
 *
 */
public class RenamingEditorTest extends TestCase {

  public void testRenameFile() throws Exception {
    Map<String, String> mappings = ImmutableMap.of("fuzzy/wuzzy", "buzzy", "olddir", "newdir");
    String inputFilename = "/tmp/olddir/foo/bar.txt";
    String outputFilename = RenamingEditor.renameFile(inputFilename, mappings);
    assertEquals("/tmp/newdir/foo/bar.txt", outputFilename);
    String inputFilename2 = "/tmp/dir/foo/bar.txt";
    String outputFilename2 = RenamingEditor.renameFile(inputFilename2, mappings);
    assertNull(outputFilename2);
    String inputFilename3 = "/tmp/fuzzy/wuzzy/wasabear/foo.txt";
    String outputFilename3 = RenamingEditor.renameFile(inputFilename3, mappings);
    assertEquals("/tmp/buzzy/wasabear/foo.txt", outputFilename3);
  }

  public void testCopyDirectoryAndRename() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;
    File srcContents = new File("/src/olddummy/file1");
    File srcContents2 = new File("/src/olddummy/file2");
    File src = new File("/src");
    File dest = new File("/dest");

    Map<String, String> mappings = ImmutableMap.of("olddummy", "newdummy");

    expect(fileSystem.isFile(src)).andReturn(false);
    expect(fileSystem.listFiles(src)).andReturn(new File[] {new File("/src/olddummy")});
    expect(fileSystem.getName(new File("/src/olddummy"))).andReturn("olddummy");
    expect(fileSystem.isDirectory(new File("/src/olddummy"))).andReturn(true);

    expect(fileSystem.isFile(new File("/src/olddummy"))).andReturn(false);
    expect(fileSystem.listFiles(new File("/src/olddummy"))).
        andReturn(new File[] {new File("/src/olddummy/file1"), new File("/src/olddummy/file2")});

    expect(fileSystem.getName(new File("/src/olddummy/file1"))).andReturn("file1");
    expect(fileSystem.isDirectory(new File("/src/olddummy/file1"))).andReturn(false);
    fileSystem.makeDirsForFile(new File("/dest/newdummy/file1"));
    fileSystem.copyFile(srcContents, new File("/dest/newdummy/file1"));

    expect(fileSystem.getName(new File("/src/olddummy/file2"))).andReturn("file2");
    expect(fileSystem.isDirectory(new File("/src/olddummy/file2"))).andReturn(false);
    fileSystem.makeDirsForFile(new File("/dest/newdummy/file2"));
    fileSystem.copyFile(srcContents2, new File("/dest/newdummy/file2"));

    control.replay();
    RenamingEditor.copyDirectoryAndRename(src, dest, mappings);
    control.verify();
  }

  public void testEdit() throws Exception {
    File codebase = new File("/codebase/");
    File oldSubFile = new File("/codebase/moe.txt");
    File newSubFile = new File("/rename_run_foo/joe.txt");

    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File renameRun = new File("/rename_run_foo");

    expect(fileSystem.getTemporaryDirectory("rename_run_")).andReturn(renameRun);
    expect(fileSystem.isFile(codebase)).andReturn(false);
    expect(fileSystem.listFiles(codebase)).andReturn(new File[] {oldSubFile});
    expect(fileSystem.getName(oldSubFile)).andReturn("moe.txt");
    expect(fileSystem.isDirectory(oldSubFile)).andReturn(false);
    fileSystem.makeDirsForFile(newSubFile);
    fileSystem.copyFile(oldSubFile, newSubFile);

    control.replay();

    new RenamingEditor("renamey", ImmutableMap.<String, String>of("moe", "joe"))
        .edit(codebase, null);

    control.verify();
  }

  public void testParseJsonMap() throws Exception {
    JsonObject jsonMap = new JsonObject();
    jsonMap.addProperty("java/com/google/devtools/", "src/");
    jsonMap.addProperty("javatests/com/google/devtools/", "tests/");
    Map<String, String> map = RenamingEditor.parseJsonMap(jsonMap);
    assertEquals(ImmutableMap.of("java/com/google/devtools/", "src/",
        "javatests/com/google/devtools/", "tests/"), map);
  }
}
