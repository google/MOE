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

package com.google.devtools.moe.client.translation.editors;

import static com.google.devtools.moe.client.config.EditorType.renamer;
import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.GsonModule;
import com.google.devtools.moe.client.config.EditorConfig;
import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class RenamingEditorTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final Gson gson = GsonModule.provideGson();
  private final ScrubberConfig scrubberConfig = gson.fromJson("{}", ScrubberConfig.class);

  public void testRenameFile_NoRegex() throws Exception {
    String mappings =
        "{"
            + "\"fuzzy/wuzzy\": \"buzzy\","
            + "\"olddir\": \"newdir\","
            + "\".*\": \"ineffectual_regex\""
            + "}";
    EditorConfig config = EditorConfig.create(renamer, scrubberConfig, "", parse(mappings), false);
    RenamingEditor renamer = new RenamingEditor(fileSystem, gson, "renamey", config);

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
    String mappings =
        "{" + "\"/old([^/]*)\": \"/brand/new$1\"," + "\"fuzzy/wuzzy\": \"buzzy\"" + "}";
    EditorConfig config = EditorConfig.create(renamer, scrubberConfig, "", parse(mappings), true);
    RenamingEditor renamer = new RenamingEditor(fileSystem, gson, "renamey", config);

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
    String mappings = "{\"olddummy\": \"newdummy\"}";
    EditorConfig config = EditorConfig.create(renamer, scrubberConfig, "", parse(mappings), false);
    RenamingEditor renamer = new RenamingEditor(fileSystem, gson, "renamey", config);

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
        Codebase.create(codebaseFile, "internal", new RepositoryExpression("ignored"));

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

    String mappings = "{\"moe\": \"joe\"}";
    EditorConfig config = EditorConfig.create(renamer, scrubberConfig, "", parse(mappings), false);
    new RenamingEditor(fileSystem, gson, "renamey", config)
        .edit(codebase, ImmutableMap.<String, String>of());

    control.verify();
  }

  private JsonObject parse(String json) {
    JsonObject mappings = new JsonParser().parse(json).getAsJsonObject();
    return mappings;
  }
}
