// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.tools;

import static org.easymock.EasyMock.expect;
import static com.google.devtools.moe.client.tools.FileDifference.Comparison;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class FileDifferenceTest extends TestCase {

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);

  @Module(overrides = true, includes = AppContextForTesting.class)
  class LocalTestModule {
    @Provides public CommandRunner commandRunner() {
      return cmd;
    }
    @Provides public FileSystem fileSystem() {
      return fileSystem;
    }
  }

  public void testExistence() throws Exception {
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();

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
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();

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
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();

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
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();

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
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();

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
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();

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
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();

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
