// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.List;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class UtilsTest extends TestCase {
  public void testFilterByRegEx() throws Exception {
    assertEquals(
        ImmutableSet.of("foo", "br"),
        Utils.filterByRegEx(ImmutableSet.of("foo", "br", "bar", "baar"),
                            ImmutableList.of("ba+r")));
  }

  public void testCheckKeys() throws Exception {
    Utils.checkKeys(
        ImmutableMap.of("foo", "bar"), ImmutableSet.of("foo", "baz"));

    try {
      Utils.checkKeys(
          ImmutableMap.of("foo", "bar"), ImmutableSet.of("baz"));
      fail();
    } catch (MoeProblem expected) {}

    Utils.checkKeys(ImmutableMap.<String, String>of(), ImmutableSet.<String>of());

    try {
      Utils.checkKeys(ImmutableMap.<String, String>of("foo", "bar"), ImmutableSet.<String>of());
      fail("Non-empty options map didn't fail on key emptiness check.");
    } catch (MoeProblem expected) {}
  }

  public void testMakeFilenamesRelative() throws Exception {
    assertEquals(
        ImmutableSet.of("bar", "baz/quux"),
        Utils.makeFilenamesRelative(
            ImmutableSet.of(
                new File("/foo/bar"),
                new File("/foo/baz/quux")),
            new File("/foo")));
    try {
      Utils.makeFilenamesRelative(
          ImmutableSet.of(new File("/foo/bar")),
          new File("/dev/null"));
      fail();
    } catch (MoeProblem p) {}
  }

  /**
   * Confirms that the expandToDirectory()-method calls the proper expand methods for
   * known archive types.
   * @throws Exception
   */
  public void testExpandToDirectory() throws Exception {
    // Set up the test environment.
    AppContextForTesting.initForTest();
    String filePath = "/foo/bar.tar";
    File file = new File(filePath);

    FileSystem mockfs = EasyMock.createMock(FileSystem.class);
    expect(mockfs.getTemporaryDirectory(EasyMock.<String>anyObject())).andReturn(new File("/test"));
    mockfs.makeDirs(EasyMock.<File>anyObject());
    EasyMock.expectLastCall().once();
    EasyMock.replay(mockfs);
    AppContext.RUN.fileSystem = mockfs;

    CommandRunner mockcmd = EasyMock.createMock(CommandRunner.class);
    expect(mockcmd.runCommand(EasyMock.<String>anyObject(),
                              EasyMock.<List<String>>anyObject(),
                              EasyMock.<String>anyObject())).andReturn(null);
    EasyMock.replay(mockcmd);
    AppContext.RUN.cmd = mockcmd;

    // Run the .expandToDirectory method.
    File directory = Utils.expandToDirectory(file);
    assertNotNull(directory);
    assertEquals("/test", directory.toString());

    EasyMock.verify(mockfs);
    EasyMock.verify(mockcmd);
  }

  /**
   * Confirms that the expandToDirectory()-method will return null when handed a unsupported
   * file extension.
   * @throws Exception
   */
  public void testUnsupportedExpandToDirectory() throws Exception {
    // Set up the test environment.
    AppContextForTesting.initForTest();
    String filePath = "/foo/bar.unsupportedArchive";
    File file = new File(filePath);

    // Run the .expandToDirectory method.
    File directory = Utils.expandToDirectory(file);
    assertNull(directory);
  }

  public void testExpandTar() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;
    fileSystem.makeDirs(new File("/dummy/path/45.expanded"));
    expect(fileSystem.getTemporaryDirectory("expanded_tar_"))
        .andReturn(new File("/dummy/path/45.expanded"));
    expect(cmd.runCommand(
        "tar",
        ImmutableList.of("-xf", "/dummy/path/45.tar"),
        "/dummy/path/45.expanded")).andReturn("");
    control.replay();
    File expanded = Utils.expandTar(new File("/dummy/path/45.tar"));
    assertEquals(new File("/dummy/path/45.expanded"), expanded);
    control.verify();
  }

  public void testCopyDirectory() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;
    File srcContents = new File("/src/dummy/file");
    File src = new File("/src");
    File dest = new File("/dest");

    fileSystem.makeDirsForFile(new File("/dest"));
    expect(fileSystem.isFile(src)).andReturn(false);
    expect(fileSystem.listFiles(src)).andReturn(new File[] {new File("/src/dummy")});
    expect(fileSystem.getName(new File("/src/dummy"))).andReturn("dummy");
    expect(fileSystem.isDirectory(new File("/src/dummy"))).andReturn(true);
    fileSystem.makeDirsForFile(new File("/dest/dummy"));
    expect(fileSystem.isFile(new File("/src/dummy"))).andReturn(false);
    expect(fileSystem.listFiles(new File("/src/dummy"))).
        andReturn(new File[] {new File("/src/dummy/file")});
    expect(fileSystem.getName(new File("/src/dummy/file"))).andReturn("file");
    expect(fileSystem.isDirectory(new File("/src/dummy/file"))).andReturn(false);
    fileSystem.makeDirsForFile(new File("/dest/dummy/file"));
    fileSystem.copyFile(srcContents, new File("/dest/dummy/file"));

    control.replay();
    Utils.copyDirectory(src, dest);
    control.verify();
  }

  public void testMakeShellScript() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;
    File script = new File("/path/to/script");

    fileSystem.write("#!/bin/sh -e\nmessage contents", script);
    fileSystem.setExecutable(script);

    control.replay();
    Utils.makeShellScript("message contents", "/path/to/script");
    control.verify();
  }
}
