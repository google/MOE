// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.testing.TestingModule;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.List;

import javax.inject.Singleton;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class UtilsTest extends TestCase {

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem fileSystem = control.createMock(FileSystem.class);
  private final CommandRunner mockcmd = control.createMock(CommandRunner.class);

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, Module.class})
  @Singleton
  interface Component extends Moe.Component {
    @Override Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module class Module {
    @Provides public CommandRunner commandRunner() {
      return mockcmd;
    }
    @Provides public FileSystem fileSystem() {
      return fileSystem;
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = DaggerUtilsTest_Component.builder().module(new Module()).build().context();
  }

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
    String filePath = "/foo/bar.tar";
    File file = new File(filePath);

    expect(fileSystem.getTemporaryDirectory(EasyMock.<String>anyObject()))
        .andReturn(new File("/test"));
    fileSystem.makeDirs(EasyMock.<File>anyObject());
    EasyMock.expectLastCall().once();
    expect(mockcmd.runCommand(EasyMock.<String>anyObject(),
                              EasyMock.<List<String>>anyObject(),
                              EasyMock.<String>anyObject())).andReturn(null);
    control.replay();
    // Run the .expandToDirectory method.
    File directory = Utils.expandToDirectory(file);
    assertNotNull(directory);
    assertEquals("/test", directory.toString());

    control.verify();
  }

  /**
   * Confirms that the expandToDirectory()-method will return null when handed a unsupported
   * file extension.
   * @throws Exception
   */
  public void testUnsupportedExpandToDirectory() throws Exception {
    String filePath = "/foo/bar.unsupportedArchive";
    File file = new File(filePath);

    // Run the .expandToDirectory method.
    File directory = Utils.expandToDirectory(file);
    assertNull(directory);
  }

  public void testExpandTar() throws Exception {
    fileSystem.makeDirs(new File("/dummy/path/45.expanded"));
    expect(fileSystem.getTemporaryDirectory("expanded_tar_"))
        .andReturn(new File("/dummy/path/45.expanded"));
    expect(mockcmd.runCommand(
        "tar",
        ImmutableList.of("-xf", "/dummy/path/45.tar"),
        "/dummy/path/45.expanded")).andReturn("");
    control.replay();
    File expanded = Utils.expandTar(new File("/dummy/path/45.tar"));
    assertEquals(new File("/dummy/path/45.expanded"), expanded);
    control.verify();
  }

  public void testCopyDirectory() throws Exception {
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
    File script = new File("/path/to/script");

    fileSystem.write("#!/bin/sh -e\nmessage contents", script);
    fileSystem.setExecutable(script);

    control.replay();
    Utils.makeShellScript("message contents", "/path/to/script");
    control.verify();
  }
}
