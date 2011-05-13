// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;
import java.io.File;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class UtilsTest extends TestCase {
  public void testFilterByRegEx() throws Exception {
    assertEquals(
        ImmutableSet.of("foo", "br"),
        Utils.filterByRegEx(ImmutableSet.of("foo", "br", "bar", "baar"), "ba+r"));
  }

  public void testCheckKeys() throws Exception {
    Utils.checkKeys(
        ImmutableMap.of("foo", "bar"), ImmutableSet.of("foo", "baz"));
    try {
      Utils.checkKeys(
          ImmutableMap.of("foo", "bar"), ImmutableSet.of("baz"));
      fail();
    } catch (MoeProblem p) {}
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

  public void testExpandTar() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
    AppContext.RUN.fileSystem = fileSystem;
    fileSystem.makeDirs(new File("/dummy/path/45.expanded"));
    expect(fileSystem.getTemporaryDirectory("expanded_tar_")).
        andReturn(new File("/dummy/path/45.expanded"));
    expect(cmd.runCommand(
        "tar",
        ImmutableList.of("-xf", "/dummy/path/45.tar"),
        "", "/dummy/path/45.expanded")).andReturn("");
    control.replay();
    File expanded = Utils.expandTar(new File("/dummy/path/45.tar"));
    assertEquals(new File("/dummy/path/45.expanded"), expanded);
    control.verify();
  }
}
