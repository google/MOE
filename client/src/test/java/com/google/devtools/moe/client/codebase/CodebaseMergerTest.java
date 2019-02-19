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

package com.google.devtools.moe.client.codebase;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.CodebaseMerger.MergeResult;
import com.google.devtools.moe.client.tools.FileDifference;
import com.google.devtools.moe.client.tools.FileDifference.ConcreteFileDiffer;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * Unit tests for the CodebaseMerger class.
 *
 * <p>Here is a diagram illustrating a merge situation. The test cases below will refer to this
 * diagram when explaining what type of case they are testing.
 *
 * <pre>
 *                                                   _____
 *                                                  |     |
 *                                                  |  7  | (mod)
 *                                                  |_____|
 *                                                     |
 *                                                     |
 *                                                     |
 *                                                     |
 *                        ____                       __|__
 *                       |    |                     |     |
 *                (dest) |1006|=====================|  6  | (orig)
 *                       |____|                     |_____|
 *
 *                    internalrepo                 publicrepo
 * </pre>
 */
@RunWith(JUnit4.class)
public class CodebaseMergerTest {
  private final FileSystem fileSystem = mock(FileSystem.class);
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final Ui ui = new Ui(stream, /* fileSystem */ null);
  private final CommandRunner cmd = mock(CommandRunner.class);
  private final FileDiffer fileDiffer = new ConcreteFileDiffer(cmd, fileSystem);

  private final Codebase orig = mock(Codebase.class);
  private final Codebase dest = mock(Codebase.class);
  private final Codebase merged = mock(Codebase.class);
  private final Codebase mod = mock(Codebase.class);

  private final File mergedCodebaseLocation = new File("merged_codebase_7");
  private final File origFile = new File("orig/foo");
  private final File destFile = new File("dest/foo");
  private final File modFile = new File("mod/foo");

  private final MergeResult.Builder resultBuilder = MergeResult.builder().setMergedCodebase(merged);

  @Before
  public void setUp() {
    when(merged.root()).thenReturn(mergedCodebaseLocation);
    when(merged.getFile("foo")).thenReturn(new File(mergedCodebaseLocation, "foo"));
    when(fileSystem.getTemporaryDirectory("merged_codebase_")).thenReturn(mergedCodebaseLocation);

    when(orig.getFile("foo")).thenReturn(origFile);
    when(dest.getFile("foo")).thenReturn(destFile);
    when(mod.getFile("foo")).thenReturn(modFile);

    when(fileSystem.isExecutable(origFile)).thenReturn(false);
    when(fileSystem.isExecutable(modFile)).thenReturn(false);
  }

  /**
   * Test generateMergedFile(...) in the case where the file exists in orig and mod but not dest. In
   * this case, the file is unchanged in orig and mod, so the file is not placed in the merged
   * codebase.
   */
  @Test
  public void testGenerateMergedFileDestDelete() throws Exception {
    when(fileSystem.exists(origFile)).thenReturn(true);
    when(fileSystem.exists(destFile)).thenReturn(false);
    when(fileSystem.exists(modFile)).thenReturn(true);

    CodebaseMerger merger = new CodebaseMerger(ui, fileSystem, cmd, fileDiffer);
    merger.generateMergedFile(orig, mod, dest, resultBuilder, "foo");

    verify(cmd)
        .runCommand(
            "",
            "diff",
            ImmutableList.of("-N", "-u", origFile.getAbsolutePath(), modFile.getAbsolutePath()));
  }

  /**
   * Test generateMergedFile(...) in the case where the file exists in orig and dest but not mod.
   * The orig version and the dest version of the file differ. This should be treated as a conflict
   * for the user to resolve.
   */
  @Test
  public void testGenerateMergedFileModDeleteConflict() throws Exception {
    FileDiffer differ = mock(FileDiffer.class);
    FileDifference difference = mock(FileDifference.class);
    when(differ.diffFiles(anyString(), any(), any())).thenReturn(difference);
    when(difference.isDifferent()).thenReturn(true);
    resultBuilder.setMergedCodebase(merged);
    when(fileSystem.exists(origFile)).thenReturn(true);
    when(fileSystem.exists(destFile)).thenReturn(true);
    when(fileSystem.exists(modFile)).thenReturn(false);

    // Simulate merging on an empty file, since /dev/null got copied over the file in this flow.
    when(cmd.runCommand(anyString(), eq("merge"), Mockito.anyListOf(String.class)))
        .thenThrow(new CommandException("merge", ImmutableList.of(), "", "", 1));

    CodebaseMerger merger = new CodebaseMerger(ui, fileSystem, cmd, differ);
    merger.generateMergedFile(orig, mod, dest, resultBuilder, "foo");

    // Expect no changes to the failed/merged files, and merge in /dev/null forcing a user conflict.
    assertThat(resultBuilder.failedFilesBuilder().build()).isEmpty();
    assertThat(resultBuilder.mergedFilesBuilder().build()).isEmpty();
  }

  /**
   * Test generateMergedFile(...) in the case when the file exists only in mod. In this case, the
   * file should simply be copied to the merged codebase.
   */
  @Test
  public void testGenerateMergedFileAddFile() throws Exception {
    File mergedFile = new File("merged_codebase_7/foo");
    FileDiffer differ = mock(FileDiffer.class);
    FileDifference difference = mock(FileDifference.class);
    when(differ.diffFiles(anyString(), any(), any())).thenReturn(difference);
    when(difference.isDifferent()).thenReturn(true);
    when(fileSystem.exists(origFile)).thenReturn(false);
    when(fileSystem.exists(destFile)).thenReturn(false);
    when(fileSystem.exists(modFile)).thenReturn(true);

    CodebaseMerger merger = new CodebaseMerger(ui, fileSystem, cmd, differ);
    merger.generateMergedFile(orig, mod, dest, resultBuilder, "foo");

    assertThat(resultBuilder.mergedFilesBuilder().build()).isEmpty();
    assertThat(resultBuilder.failedFilesBuilder().build()).isEmpty();

    verify(fileSystem).makeDirsForFile(mergedFile);
    verify(fileSystem).copyFile(modFile, mergedFile);
  }

  /**
   * Test generateMergedFile(...) in the most ideal case where the file exists in all three
   * codebases and there is no conflict.
   */
  @Test
  public void testGenerateMergedFileClean() throws Exception {
    File mergedFile = new File("merged_codebase_7/foo");

    when(fileSystem.exists(origFile)).thenReturn(true);
    when(fileSystem.exists(destFile)).thenReturn(true);
    when(fileSystem.exists(modFile)).thenReturn(true);

    List<String> mergeArgs =
        ImmutableList.of(
            mergedFile.getAbsolutePath(), origFile.getAbsolutePath(), modFile.getAbsolutePath());

    when(cmd.runCommand(mergedCodebaseLocation.getAbsolutePath(), "merge", mergeArgs))
        .thenReturn("");

    CodebaseMerger merger = new CodebaseMerger(ui, fileSystem, cmd, null);
    merger.generateMergedFile(orig, mod, dest, resultBuilder, "foo");

    assertThat(resultBuilder.failedFilesBuilder().build()).isEmpty();
    assertThat(resultBuilder.mergedFilesBuilder().build())
        .containsExactly(mergedFile.getAbsolutePath());

    verify(fileSystem).makeDirsForFile(mergedFile);
    verify(fileSystem).copyFile(destFile, mergedFile);
    verify(cmd).runCommand(mergedCodebaseLocation.getAbsolutePath(), "merge", mergeArgs);
  }

  /**
   * Test generateMergedFile(...) in the case where the file exists in all three codebases but there
   * is a conflict when merging.
   */
  @Test
  public void testGenerateMergedFileConflict() throws Exception {
    File mergedFile = new File("merged_codebase_7/foo");
    when(fileSystem.exists(origFile)).thenReturn(true);
    when(fileSystem.exists(destFile)).thenReturn(true);
    when(fileSystem.exists(modFile)).thenReturn(true);

    List<String> mergeArgs =
        ImmutableList.of(
            mergedFile.getAbsolutePath(), origFile.getAbsolutePath(), modFile.getAbsolutePath());

    when(cmd.runCommand(mergedCodebaseLocation.getAbsolutePath(), "merge", mergeArgs))
        .thenThrow(new CommandRunner.CommandException("merge", mergeArgs, "", "", 1));

    CodebaseMerger merger = new CodebaseMerger(ui, fileSystem, cmd, null);
    merger.generateMergedFile(orig, mod, dest, resultBuilder, "foo");

    assertThat(resultBuilder.mergedFilesBuilder().build()).isEmpty();
    assertThat(resultBuilder.failedFilesBuilder().build()).contains(mergedFile.getAbsolutePath());
    verify(fileSystem).makeDirsForFile(mergedFile);
    verify(fileSystem).copyFile(destFile, mergedFile);
    verify(cmd).runCommand(mergedCodebaseLocation.getAbsolutePath(), "merge", mergeArgs);
  }

  /**
   * Test generateMergedFile(...) in the case where the file only exists in dest. The file should
   * appear in the merged codebase unchanged.
   */
  @Test
  public void testGenerateMergedFileDestOnly() throws Exception {
    when(fileSystem.exists(origFile)).thenReturn(false);
    when(fileSystem.exists(destFile)).thenReturn(true);
    when(fileSystem.exists(modFile)).thenReturn(false);

    CodebaseMerger merger = new CodebaseMerger(ui, fileSystem, cmd, null);
    merger.generateMergedFile(orig, mod, dest, resultBuilder, "foo");

    assertThat(resultBuilder.failedFilesBuilder().build()).isEmpty();
    assertThat(resultBuilder.mergedFilesBuilder().build()).isEmpty();
  }

  /**
   * Test generateMergedFile(...) in the case where the file exists in mod and dest but not orig. In
   * this case, the mod version and dest version are the same so there should be no conflict. The
   * file should remain unchanged in the merged codebase.
   */
  @Test
  public void testGenerateMergedFileNoOrig() throws Exception {
    File mergedFile = new File("merged_codebase_7/foo");
    when(fileSystem.exists(origFile)).thenReturn(false);
    when(fileSystem.exists(destFile)).thenReturn(true);
    when(fileSystem.exists(modFile)).thenReturn(true);

    List<String> mergeArgs =
        ImmutableList.of(mergedFile.getAbsolutePath(), "/dev/null", modFile.getAbsolutePath());

    when(cmd.runCommand(mergedCodebaseLocation.getAbsolutePath(), "merge", mergeArgs))
        .thenReturn("");

    CodebaseMerger merger = new CodebaseMerger(ui, fileSystem, cmd, null);
    merger.generateMergedFile(orig, mod, dest, resultBuilder, "foo");

    assertThat(resultBuilder.failedFilesBuilder().build()).isEmpty();
    assertThat(resultBuilder.mergedFilesBuilder().build())
        .containsExactly(mergedFile.getAbsolutePath());
    verify(fileSystem).makeDirsForFile(mergedFile);
    verify(fileSystem).copyFile(destFile, mergedFile);
    verify(cmd).runCommand(mergedCodebaseLocation.getAbsolutePath(), "merge", mergeArgs);
  }

  /**
   * Test generateMergedFile(...) in the case where the file exists in mod and dest but not orig. In
   * this case, the mod version and dest version are different so a conflict should occur when
   * merging so that the user can resolved the discrepancy.
   */
  @Test
  public void testGenerateMergedFileNoOrigConflict() throws Exception {
    File mergedFile = new File("merged_codebase_7/foo");
    when(fileSystem.exists(origFile)).thenReturn(false);
    when(fileSystem.exists(destFile)).thenReturn(true);
    when(fileSystem.exists(modFile)).thenReturn(true);

    List<String> mergeArgs =
        ImmutableList.of(
            mergedFile.getAbsolutePath(), origFile.getAbsolutePath(), modFile.getAbsolutePath());

    // Fail this merge command.
    when(cmd.runCommand(anyString(), eq("merge"), Mockito.anyListOf(String.class)))
        .thenThrow(new CommandRunner.CommandException("merge", mergeArgs, "", "", 1));

    CodebaseMerger merger = new CodebaseMerger(ui, fileSystem, cmd, null);
    merger.generateMergedFile(orig, mod, dest, resultBuilder, "foo");

    // Verify that the file was copied, and that the merge command was executed.
    verify(fileSystem).makeDirsForFile(mergedFile);
    verify(fileSystem).copyFile(destFile, mergedFile);
    verify(cmd).runCommand(anyString(), eq("merge"), Mockito.anyListOf(String.class));
    assertThat(resultBuilder.mergedFilesBuilder().build()).isEmpty();
    assertThat(resultBuilder.failedFilesBuilder().build())
        .containsExactly(mergedFile.getAbsolutePath());
  }

  /** Test for merge() */
  @Test
  public void testMerge() throws Exception {
    Ui ui = mock(Ui.class);

    when(orig.root()).thenReturn(new File("orig"));
    when(dest.root()).thenReturn(new File("dest"));
    when(mod.root()).thenReturn(new File("mod"));
    when(fileSystem.exists(origFile)).thenReturn(true);
    when(fileSystem.exists(destFile)).thenReturn(true);
    when(fileSystem.exists(modFile)).thenReturn(true);
    when(fileSystem.findFiles(new File("dest"))).thenReturn(ImmutableSet.of(destFile));
    when(fileSystem.findFiles(new File("mod"))).thenReturn(ImmutableSet.of(modFile));

    File mergedFile = new File("merged_codebase_7/foo");

    List<String> mergeArgs =
        ImmutableList.of(
            mergedFile.getAbsolutePath(), origFile.getAbsolutePath(), modFile.getAbsolutePath());

    when(cmd.runCommand(mergedCodebaseLocation.getAbsolutePath(), "merge", mergeArgs))
        .thenReturn("");

    CodebaseMerger merger = new CodebaseMerger(ui, fileSystem, cmd, null);
    MergeResult result = merger.merge(orig, mod, dest);

    assertThat(result.mergedFiles()).contains(mergedFile.getAbsolutePath());
    assertThat(result.failedFiles()).isEmpty();

    verify(fileSystem).makeDirsForFile(mergedFile);
    verify(fileSystem).copyFile(destFile, mergedFile);
    verify(cmd).runCommand(mergedCodebaseLocation.getAbsolutePath(), "merge", mergeArgs);

    // verify call to report()
    verify(ui)
        .message("Merged codebase generated at: %s", mergedCodebaseLocation.getAbsolutePath());
    verify(ui).message("%d files merged successfully. No merge conflicts.", 1);
  }
}
