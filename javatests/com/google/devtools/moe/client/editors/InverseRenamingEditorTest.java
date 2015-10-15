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
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.FakeProjectContext;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.testing.TestingModule;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.io.IOException;

import javax.inject.Singleton;

public class InverseRenamingEditorTest extends TestCase {

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFs = control.createMock(FileSystem.class);

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, SystemCommandRunner.Module.class, Module.class})
  @Singleton
  interface Component {
    Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module
  class Module {
    @Provides
    public FileSystem filesystem() {
      return mockFs;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE =
        DaggerInverseRenamingEditorTest_Component.builder().module(new Module()).build().context();
  }

  public void testEdit() throws Exception {
    ProjectContext context = new FakeProjectContext();

    InverseRenamingEditor inverseRenamey =
        new InverseRenamingEditor(
            new RenamingEditor(
                "renamey", ImmutableMap.of("internal_root", "public_root"), false /*useRegex*/));

    Codebase input =
        new Codebase(mockFs, new File("/input"), "public", new RepositoryExpression("input"));
    Codebase destination =
        new Codebase(
            mockFs, new File("/destination"), "public", new RepositoryExpression("destination"));

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
            input, null /*referenceFrom*/, destination, context, ImmutableMap.<String, String>of());
    assertEquals(new File("/output"), inverseRenamed.getPath());
    control.verify();
  }

  private void expectCopy(FileSystem mockFs, String srcPath, String destPath) throws IOException {
    mockFs.makeDirsForFile(new File(destPath));
    mockFs.copyFile(new File(srcPath), new File(destPath));
  }
}
