// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.FileCodebaseCreator;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Tests for the FileCodebaseCreator class.
 *
 */
public class FileCodebaseCreatorTest extends TestCase {

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockfs = control.createMock(FileSystem.class);

  @Module(overrides = true, includes = AppContextForTesting.class)
  class LocalTestModule {
    @Provides public FileSystem fileSystem() {
      return mockfs;
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    ObjectGraph graph = ObjectGraph.create(new LocalTestModule());
    graph.injectStatics();
  }


  private void expectDirCopy(File src, File dest) throws Exception {
    expect(mockfs.exists(EasyMock.eq(src))).andReturn(true);
    expect(mockfs.isDirectory(EasyMock.eq(src))).andReturn(true);
    expect(mockfs.getTemporaryDirectory("file_codebase_copy_")).andReturn(dest);
    // Short-circuit Utils.copyDirectory().
    mockfs.makeDirsForFile(dest);
    expect(mockfs.isFile(src)).andReturn(true);
    mockfs.copyFile(src, dest);
  }

  /**
   * Confirms that the "create"-method does validate the option list.
   * @throws CodebaseCreationError
   */
  public void testValidationCreate() throws Exception {
    FileCodebaseCreator cc = new FileCodebaseCreator();

    // Validate parameters.
    ImmutableMap<String, String> s;
    try {
      cc.create(ImmutableMap.<String, String>of());
      fail("Method does not check for required options.");
    } catch (CodebaseCreationError expected) {}

    try {
      cc.create(ImmutableMap.<String, String>of("path", "FooBar", "unknown", "123"));
      fail("Method does not check for unsupported options.");
    } catch (MoeProblem expected) {}
  }

  /**
   * Confirms that the "create"-method returns a codebase when no project space is defined.
   * @throws CodebaseCreationError
   */
  public void testCreateWithoutProjectSpace() throws Exception {
    String folder = "/foo/bar";
    File fileFolder = new File(folder);
    expectDirCopy(fileFolder, new File("/tmp/copy"));

    control.replay();
    FileCodebaseCreator cc = new FileCodebaseCreator();
    Codebase codebase = cc.create(ImmutableMap.<String, String>of("path", folder));
    assertNotNull(codebase);
    assertEquals("public", codebase.getProjectSpace());
    control.verify();
  }

  /**
   * Confirms that the "create"-method returns a codebase when a project space is defined.
   * @throws CodebaseCreationError
   */
  public void testCreateWithProjectSpace() throws Exception {
    String folder = "/foo/bar";
    File fileFolder = new File(folder);

    expectDirCopy(fileFolder, new File("/tmp/copy"));

    control.replay();
    FileCodebaseCreator cc = new FileCodebaseCreator();
    Codebase codebase = cc.create(ImmutableMap.<String, String>of("path", folder,
                                                                 "projectspace", "internal"));
    control.verify();

    assertNotNull(codebase);
    assertEquals("internal", codebase.getProjectSpace());
  }

  /**
   * Tests whether the getCodebasePath() method works with a directory.
   * @throws CodebaseCreationError
   */
  public void testGetCodebasePathWithDirectory() throws Exception {
    String folder = "/foo/bar";
    File fileFolder = new File(folder);
    File copyLocation = new File("/tmp/copy");

    expectDirCopy(fileFolder, copyLocation);

    control.replay();
    FileCodebaseCreator cc = new FileCodebaseCreator();
    File newPath = FileCodebaseCreator.getCodebasePath(fileFolder);
    control.verify();

    assertEquals(newPath, copyLocation);
  }

  /**
   * Tests whether the getCodebasePath() method works with an unknown file type.
   * @throws CodebaseCreationError
   */
  public void testGetCodebasePathWithUnknownFile() throws Exception {
    String filePath = "/foo/bar.sth";
    File fileFolder = new File(filePath);

    expect(mockfs.exists(EasyMock.eq(fileFolder))).andReturn(true);
    expect(mockfs.isDirectory(EasyMock.eq(fileFolder))).andReturn(false);
    expect(mockfs.isFile(EasyMock.eq(fileFolder))).andReturn(true);

    control.replay();
    try {
      FileCodebaseCreator.getCodebasePath(fileFolder);
      fail("getCodebasePath() did not throw an exception for an unsupported file type.");
    } catch (CodebaseCreationError expected) {}
    control.verify();
  }

  /**
   * Tests whether the getCodebasePath() method works with a .tar.
   */
  public void testGetCodebasePathWithKnownFile()
      throws CodebaseCreationError, IOException, CommandException {
    String filePath = "/foo/bar.tar";
    File fileFolder = new File(filePath);

    expect(mockfs.exists(EasyMock.eq(fileFolder))).andReturn(true);
    expect(mockfs.isDirectory(EasyMock.eq(fileFolder))).andReturn(false);
    expect(mockfs.isFile(EasyMock.eq(fileFolder))).andReturn(true);
    expect(mockfs.getTemporaryDirectory(EasyMock.<String>anyObject())).andReturn(new File("sth"));
    mockfs.makeDirs(EasyMock.<File>anyObject());
    EasyMock.expectLastCall().atLeastOnce();

    CommandRunner mockcmd = EasyMock.createMock(CommandRunner.class);
    expect(mockcmd.runCommand(EasyMock.<String>anyObject(),
                              EasyMock.<List<String>>anyObject(),
                              EasyMock.<String>anyObject())).andReturn(null);
    EasyMock.replay(mockcmd);
    AppContext.RUN.cmd = mockcmd;

    control.replay();
    File codebasePath = FileCodebaseCreator.getCodebasePath(fileFolder);
    control.verify();

    assertNotNull(codebasePath);
  }
}
