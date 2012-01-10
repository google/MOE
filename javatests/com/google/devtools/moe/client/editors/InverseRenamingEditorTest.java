// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.io.IOException;

/**
 * Unit tests for inverse renaming
 *
 */
public class InverseRenamingEditorTest extends TestCase {

  public void testEdit() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem mockFs = control.createMock(FileSystem.class);
    AppContext.RUN.fileSystem = mockFs;
    ProjectContext context = ProjectContext.builder().build();

    InverseRenamingEditor inverseRenamey = new InverseRenamingEditor(
        new RenamingEditor(
            "renamey", ImmutableMap.of("internal_root", "public_root"), false /*useRegex*/));

    Codebase input = new Codebase(new File("/input"), "public", new RepositoryExpression("input"));
    Codebase destination =
        new Codebase(new File("/destination"), "public", new RepositoryExpression("destination"));

    expect(mockFs.getTemporaryDirectory("inverse_rename_run_")).andReturn(new File("/output"));

    expect(mockFs.findFiles(new File("/input"))).andReturn(ImmutableSet.of(
        new File("/input/toplevel.txt"),
        new File("/input/public_root/1.txt"),
        new File("/input/public_root/new.txt"),
        new File("/input/public_root/inner1/inner2/innernew.txt")));

    expect(mockFs.findFiles(new File("/destination"))).andReturn(ImmutableSet.of(
        new File("/destination/internal_root/1.txt")));

    expectCopy(mockFs, "/input/toplevel.txt", "/output/toplevel.txt");
    expectCopy(mockFs, "/input/public_root/1.txt", "/output/internal_root/1.txt");
    expectCopy(mockFs, "/input/public_root/new.txt", "/output/internal_root/new.txt");
    expectCopy(mockFs, "/input/public_root/inner1/inner2/innernew.txt",
                       "/output/internal_root/inner1/inner2/innernew.txt");

    control.replay();
    Codebase inverseRenamed = inverseRenamey.inverseEdit(
        input, null /*referenceFrom*/, destination, context, ImmutableMap.<String, String>of());
    assertEquals(new File("/output"), inverseRenamed.getPath());
    control.verify();
  }

  private void expectCopy(FileSystem mockFs, String srcPath, String destPath) throws IOException {
    mockFs.makeDirsForFile(new File(destPath));
    mockFs.copyFile(new File(srcPath), new File(destPath));
  }
}
