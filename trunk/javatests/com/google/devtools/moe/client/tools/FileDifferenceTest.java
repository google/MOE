// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.tools;

import static org.easymock.EasyMock.expect;
import static com.google.devtools.moe.client.tools.FileDifference.Comparison;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class FileDifferenceTest extends TestCase {
  public void testExistence() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File file1 = new File("/1/foo");
    File file2 = new File("/2/foo");

    expect(fileSystem.exists(file1)).andReturn(true);
    expect(fileSystem.exists(file2)).andReturn(false);
    expect(fileSystem.isExecutable(file1)).andReturn(false);
    expect(fileSystem.isExecutable(file2)).andReturn(false);
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/1/foo", "/2/foo"), "")).andThrow(
        new CommandRunner.CommandException(
            "diff", ImmutableList.of("-N", "/1/foo", "/2/foo"), "foo", "", 1));

    control.replay();
    FileDifference d = FileDifference.CONCRETE_FILE_DIFFER.diffFiles("foo", file1, file2);
    control.verify();
    assertEquals(Comparison.ONLY1, d.existence);
    assertEquals(Comparison.SAME, d.executability);
    assertEquals("foo", d.contentDiff);
  }

  public void testExistence2() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File file1 = new File("/1/foo");
    File file2 = new File("/2/foo");

    expect(fileSystem.exists(file1)).andReturn(false);
    expect(fileSystem.exists(file2)).andReturn(true);
    expect(fileSystem.isExecutable(file1)).andReturn(false);
    expect(fileSystem.isExecutable(file2)).andReturn(false);
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/1/foo", "/2/foo"), "")).andThrow(
        new CommandRunner.CommandException(
            "diff", ImmutableList.of("-N", "/1/foo", "/2/foo"), "foo", "", 1));

    control.replay();
    FileDifference d = FileDifference.CONCRETE_FILE_DIFFER.diffFiles("foo", file1, file2);
    control.verify();
    assertEquals(Comparison.ONLY2, d.existence);
    assertEquals(Comparison.SAME, d.executability);
    assertEquals("foo", d.contentDiff);
  }

  public void testExecutability() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File file1 = new File("/1/foo");
    File file2 = new File("/2/foo");

    expect(fileSystem.exists(file1)).andReturn(true);
    expect(fileSystem.exists(file2)).andReturn(true);
    expect(fileSystem.isExecutable(file1)).andReturn(true);
    expect(fileSystem.isExecutable(file2)).andReturn(false);
    expect(cmd.runCommand("diff",
                          ImmutableList.of("-N", "/1/foo", "/2/foo"), "")).andReturn("");

    control.replay();
    FileDifference d = FileDifference.CONCRETE_FILE_DIFFER.diffFiles("foo", file1, file2);
    control.verify();
    assertEquals(Comparison.SAME, d.existence);
    assertEquals(Comparison.ONLY1, d.executability);
    assertEquals(null, d.contentDiff);
  }

  public void testExecutability2() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File file1 = new File("/1/foo");
    File file2 = new File("/2/foo");

    expect(fileSystem.exists(file1)).andReturn(true);
    expect(fileSystem.exists(file2)).andReturn(true);
    expect(fileSystem.isExecutable(file1)).andReturn(false);
    expect(fileSystem.isExecutable(file2)).andReturn(true);
    expect(cmd.runCommand("diff",
                          ImmutableList.of("-N", "/1/foo", "/2/foo"), "")).andReturn("");

    control.replay();
    FileDifference d = FileDifference.CONCRETE_FILE_DIFFER.diffFiles("foo", file1, file2);
    control.verify();
    assertEquals(Comparison.SAME, d.existence);
    assertEquals(Comparison.ONLY2, d.executability);
    assertEquals(null, d.contentDiff);
  }

  public void testContents() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File file1 = new File("/1/foo");
    File file2 = new File("/2/foo");

    expect(fileSystem.exists(file1)).andReturn(true);
    expect(fileSystem.exists(file2)).andReturn(true);
    expect(fileSystem.isExecutable(file1)).andReturn(true);
    expect(fileSystem.isExecutable(file2)).andReturn(true);
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/1/foo", "/2/foo"), "")).andThrow(
        new CommandRunner.CommandException(
            "diff", ImmutableList.of("-N", "/1/foo", "/2/foo"), "foo", "", 1));

    control.replay();
    FileDifference d = FileDifference.CONCRETE_FILE_DIFFER.diffFiles("foo", file1, file2);
    control.verify();
    assertEquals(Comparison.SAME, d.existence);
    assertEquals(Comparison.SAME, d.executability);
    assertEquals("foo", d.contentDiff);
  }

  public void testExecutabilityAndContents() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File file1 = new File("/1/foo");
    File file2 = new File("/2/foo");

    expect(fileSystem.exists(file1)).andReturn(true);
    expect(fileSystem.exists(file2)).andReturn(true);
    expect(fileSystem.isExecutable(file1)).andReturn(true);
    expect(fileSystem.isExecutable(file2)).andReturn(false);
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/1/foo", "/2/foo"), "")).andThrow(
        new CommandRunner.CommandException(
            "diff", ImmutableList.of("-N", "/1/foo", "/2/foo"), "foo", "", 1));

    control.replay();
    FileDifference d = FileDifference.CONCRETE_FILE_DIFFER.diffFiles("foo", file1, file2);
    control.verify();
    assertEquals(Comparison.SAME, d.existence);
    assertEquals(Comparison.ONLY1, d.executability);
    assertEquals("foo", d.contentDiff);
  }

  public void testIdentical() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;

    File file1 = new File("/1/foo");
    File file2 = new File("/2/foo");

    expect(fileSystem.exists(file1)).andReturn(true);
    expect(fileSystem.exists(file2)).andReturn(true);
    expect(fileSystem.isExecutable(file1)).andReturn(false);
    expect(fileSystem.isExecutable(file2)).andReturn(false);
    expect(cmd.runCommand("diff",
                          ImmutableList.of("-N", "/1/foo", "/2/foo"), "")).andReturn("");

    control.replay();
    FileDifference d = FileDifference.CONCRETE_FILE_DIFFER.diffFiles("foo", file1, file2);
    control.verify();
    assertFalse(d.isDifferent());
  }
}
