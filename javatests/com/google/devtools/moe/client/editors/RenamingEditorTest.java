// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.gson.JsonObject;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.Map;

/**
 */
public class RenamingEditorTest extends TestCase {

  public void testRenameFile_NoRegex() throws Exception {
    RenamingEditor renamer = new RenamingEditor(
        "renamey",
        ImmutableMap.of("fuzzy/wuzzy", "buzzy", "olddir", "newdir", ".*", "ineffectual_regex"),
        false /*useRegex*/);

    // Leading '/' should be trimmed.
    assertEquals("tmp/newdir/foo/bar.txt", renamer.renameFile("/tmp/olddir/foo/bar.txt"));

    assertEquals("tmp/buzzy/wasabear/foo.txt",
                 renamer.renameFile("tmp/fuzzy/wuzzy/wasabear/foo.txt"));

    try {
      renamer.renameFile("/tmp/dir/foo/bar.txt");
      fail("Renamer didn't fail on un-renamable path.");
    } catch (MoeProblem expected) {}
  }

  public void testRenameFile_Regex() throws Exception {
    RenamingEditor renamer = new RenamingEditor(
        "renamey",
        ImmutableMap.of("/old([^/]*)", "/brand/new$1", "fuzzy/wuzzy", "buzzy"),
        true /*useRegex*/);

    assertEquals("tmp/brand/newdir/foo/bar.txt", renamer.renameFile("/tmp/olddir/foo/bar.txt"));

    assertEquals("tmp/buzzy/wasabear/foo.txt",
                 renamer.renameFile("tmp/fuzzy/wuzzy/wasabear/foo.txt"));

    try {
      renamer.renameFile("/tmp/moldydir/foo/bar.txt");
      fail("Renamer didn't fail on un-renamable path.");
    } catch (MoeProblem expected) {}
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

    RenamingEditor renamer = new RenamingEditor(
        "renamey", ImmutableMap.of("olddummy", "newdummy"), false);

    expect(fileSystem.isDirectory(src)).andReturn(true);
    expect(fileSystem.listFiles(src)).andReturn(new File[] {new File("/src/olddummy")});

    expect(fileSystem.isDirectory(new File("/src/olddummy"))).andReturn(true);
    expect(fileSystem.listFiles(new File("/src/olddummy"))).
        andReturn(new File[] {new File("/src/olddummy/file1"), new File("/src/olddummy/file2")});

    expect(fileSystem.isDirectory(new File("/src/olddummy/file1"))).andReturn(false);
    fileSystem.makeDirsForFile(new File("/dest/newdummy/file1"));
    fileSystem.copyFile(srcContents, new File("/dest/newdummy/file1"));

    expect(fileSystem.isDirectory(new File("/src/olddummy/file2"))).andReturn(false);
    fileSystem.makeDirsForFile(new File("/dest/newdummy/file2"));
    fileSystem.copyFile(srcContents2, new File("/dest/newdummy/file2"));

    control.replay();
    renamer.copyDirectoryAndRename(src, src, dest);
    control.verify();
  }


  public void testEdit() throws Exception {
    File codebaseFile = new File("/codebase/");
    Codebase codebase = new Codebase(codebaseFile,
                                     "internal",
                                     null /* CodebaseExpression is not needed here. */);

    File oldSubFile = new File("/codebase/moe.txt");
    File renameRun = new File("/rename_run_foo");
    File newSubFile = new File(renameRun, "joe.txt");

    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    expect(fileSystem.getTemporaryDirectory("rename_run_")).andReturn(renameRun);

    expect(fileSystem.isDirectory(codebaseFile)).andReturn(true);
    expect(fileSystem.listFiles(codebaseFile)).andReturn(new File[] {oldSubFile});
    expect(fileSystem.isDirectory(oldSubFile)).andReturn(false);
    fileSystem.makeDirsForFile(newSubFile);
    fileSystem.copyFile(oldSubFile, newSubFile);

    control.replay();

    new RenamingEditor("renamey", ImmutableMap.of("moe", "joe"), false)
        .edit(codebase,
              null /* this edit doesn't require a ProjectContext */,
              ImmutableMap.<String, String>of() /* this edit doesn't require options */);

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
