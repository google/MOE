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
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.GsonModule;
import com.google.devtools.moe.client.config.EditorConfig;
import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class InverseRenamingEditorTest extends TestCase {

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFs = control.createMock(FileSystem.class);
  private final Gson gson = GsonModule.provideGson();
  private final ScrubberConfig scrubberConfig = gson.fromJson("{}", ScrubberConfig.class);

  public void testEdit() throws Exception {
    JsonObject mappings =
        new JsonParser().parse("{\"internal_root\": \"public_root\"}").getAsJsonObject();
    EditorConfig config = EditorConfig.create(renamer, scrubberConfig, "", mappings, false);
    RenamingEditor inverseRenamey = new RenamingEditor(mockFs, gson, "renamey", config);

    Codebase input =
        Codebase.create(new File("/input"), "public", new RepositoryExpression("input"));
    Codebase destination =
        Codebase.create(
            new File("/destination"), "public", new RepositoryExpression("destination"));

    expect(mockFs.getTemporaryDirectory("inverse_rename_run_")).andReturn(new File("/output"));

    expect(mockFs.findFiles(new File("/input")))
        .andReturn(
            ImmutableSet.of(
                new File("/input/toplevel.txt"),
                new File("/input/public_root/1.txt"),
                new File("/input/public_root/new.txt"),
                new File("/input/public_root/inner1/inner2/innernew.txt")));

    expect(mockFs.findFiles(new File("/destination")))
        .andReturn(ImmutableSet.of(new File("/destination/internal_root/1.txt")));

    expectCopy(mockFs, "/input/toplevel.txt", "/output/toplevel.txt");
    expectCopy(mockFs, "/input/public_root/1.txt", "/output/internal_root/1.txt");
    expectCopy(mockFs, "/input/public_root/new.txt", "/output/internal_root/new.txt");
    expectCopy(
        mockFs,
        "/input/public_root/inner1/inner2/innernew.txt",
        "/output/internal_root/inner1/inner2/innernew.txt");

    control.replay();
    Codebase inverseRenamed =
        inverseRenamey.inverseEdit(
            input, null /*referenceFrom*/, destination, ImmutableMap.<String, String>of());
    assertEquals(new File("/output"), inverseRenamed.root());
    control.verify();
  }

  private void expectCopy(FileSystem mockFs, String srcPath, String destPath) throws IOException {
    mockFs.makeDirsForFile(new File(destPath));
    mockFs.copyFile(new File(srcPath), new File(destPath));
  }
}
