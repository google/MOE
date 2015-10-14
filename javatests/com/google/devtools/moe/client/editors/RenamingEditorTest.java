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

package com.google.devtools.moe.client.editors;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.testing.TestingModule;
import com.google.gson.JsonObject;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.Map;

import javax.inject.Singleton;

/**
 */
public class RenamingEditorTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, Module.class})
  @Singleton
  interface Component {
    Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module
  class Module {
    @Provides
    public CommandRunner cmd() {
      return cmd;
    }

    @Provides
    public FileSystem filesystem() {
      return fileSystem;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE =
        DaggerRenamingEditorTest_Component.builder().module(new Module()).build().context();
  }

  public void testRenameFile_NoRegex() throws Exception {
    RenamingEditor renamer =
        new RenamingEditor(
            "renamey",
            ImmutableMap.of("fuzzy/wuzzy", "buzzy", "olddir", "newdir", ".*", "ineffectual_regex"),
            false /*useRegex*/);

    // Leading '/' should be trimmed.
    assertEquals("tmp/newdir/foo/bar.txt", renamer.renameFile("/tmp/olddir/foo/bar.txt"));

    assertEquals(
        "tmp/buzzy/wasabear/foo.txt", renamer.renameFile("tmp/fuzzy/wuzzy/wasabear/foo.txt"));

    try {
      renamer.renameFile("/tmp/dir/foo/bar.txt");
      fail("Renamer didn't fail on un-renamable path.");
    } catch (MoeProblem expected) {
    }
  }

  public void testRenameFile_Regex() throws Exception {
    RenamingEditor renamer =
        new RenamingEditor(
            "renamey",
            ImmutableMap.of("/old([^/]*)", "/brand/new$1", "fuzzy/wuzzy", "buzzy"),
            true /*useRegex*/);

    assertEquals("tmp/brand/newdir/foo/bar.txt", renamer.renameFile("/tmp/olddir/foo/bar.txt"));

    assertEquals(
        "tmp/buzzy/wasabear/foo.txt", renamer.renameFile("tmp/fuzzy/wuzzy/wasabear/foo.txt"));

    try {
      renamer.renameFile("/tmp/moldydir/foo/bar.txt");
      fail("Renamer didn't fail on un-renamable path.");
    } catch (MoeProblem expected) {
    }
  }

  public void testCopyDirectoryAndRename() throws Exception {
    File srcContents = new File("/src/olddummy/file1");
    File srcContents2 = new File("/src/olddummy/file2");
    File src = new File("/src");
    File dest = new File("/dest");

    RenamingEditor renamer =
        new RenamingEditor("renamey", ImmutableMap.of("olddummy", "newdummy"), false);

    expect(fileSystem.isDirectory(src)).andReturn(true);
    expect(fileSystem.listFiles(src)).andReturn(new File[] {new File("/src/olddummy")});

    expect(fileSystem.isDirectory(new File("/src/olddummy"))).andReturn(true);
    expect(fileSystem.listFiles(new File("/src/olddummy")))
        .andReturn(new File[] {new File("/src/olddummy/file1"), new File("/src/olddummy/file2")});

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
    Codebase codebase =
        new Codebase(
            fileSystem,
            codebaseFile,
            "internal",
            null /* CodebaseExpression is not needed here. */);

    File oldSubFile = new File("/codebase/moe.txt");
    File renameRun = new File("/rename_run_foo");
    File newSubFile = new File(renameRun, "joe.txt");

    expect(fileSystem.getTemporaryDirectory("rename_run_")).andReturn(renameRun);

    expect(fileSystem.isDirectory(codebaseFile)).andReturn(true);
    expect(fileSystem.listFiles(codebaseFile)).andReturn(new File[] {oldSubFile});
    expect(fileSystem.isDirectory(oldSubFile)).andReturn(false);
    fileSystem.makeDirsForFile(newSubFile);
    fileSystem.copyFile(oldSubFile, newSubFile);

    control.replay();

    new RenamingEditor("renamey", ImmutableMap.of("moe", "joe"), false)
        .edit(
            codebase,
            null /* this edit doesn't require a ProjectContext */,
            ImmutableMap.<String, String>of() /* this edit doesn't require options */);

    control.verify();
  }

  public void testParseJsonMap() throws Exception {
    JsonObject jsonMap = new JsonObject();
    jsonMap.addProperty("java/com/google/devtools/", "src/");
    jsonMap.addProperty("javatests/com/google/devtools/", "tests/");
    Map<String, String> map = RenamingEditor.parseJsonMap(jsonMap);
    assertEquals(
        ImmutableMap.of(
            "java/com/google/devtools/", "src/", "javatests/com/google/devtools/", "tests/"),
        map);
  }
}
