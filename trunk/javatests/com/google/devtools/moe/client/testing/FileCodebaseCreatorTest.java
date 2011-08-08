// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.testing;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Tests for the FileCodebaseCreator-class.
 */
public class FileCodebaseCreatorTest extends TestCase {
  /**
   * Confirms that the "create"-method does validate the option list.
   * @throws CodebaseCreationError
   */
  public void testValidationCreate() throws CodebaseCreationError {
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
  public void testCreateWithoutProjectSpace() throws CodebaseCreationError {
    // Set up mocks.
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    String folder = "/foo/bar";
    File fileFolder = new File(folder);

    FileSystem mockfs = control.createMock(FileSystem.class);
    expect(mockfs.exists(EasyMock.eq(fileFolder))).andReturn(true);
    expect(mockfs.isDirectory(EasyMock.eq(fileFolder))).andReturn(true);
    control.replay();
    AppContext.RUN.fileSystem = mockfs;

    // Run the .create method.
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
  public void testCreateWithProjectSpace() throws CodebaseCreationError {
    // Set up mocks.
    AppContextForTesting.initForTest();
    String folder = "/foo/bar";
    File fileFolder = new File(folder);

    FileSystem mockfs = EasyMock.createMock(FileSystem.class);
    expect(mockfs.exists(EasyMock.eq(fileFolder))).andReturn(true);
    expect(mockfs.isDirectory(EasyMock.eq(fileFolder))).andReturn(true);
    EasyMock.replay(mockfs);
    AppContext.RUN.fileSystem = mockfs;

    // Run the .create method.
    FileCodebaseCreator cc = new FileCodebaseCreator();
    Codebase codebase = cc.create(ImmutableMap.<String, String>of("path", folder,
                                                                 "projectspace", "internal"));
    assertNotNull(codebase);
    assertEquals("internal", codebase.getProjectSpace());
    EasyMock.verify(mockfs);
  }

  /**
   * Tests whether the getCodebasePath() method works with a directory.
   * @throws CodebaseCreationError
   */
  public void testGetCodebasePathWithDirectory() throws CodebaseCreationError {
    // Set up mocks.
    AppContextForTesting.initForTest();
    String folder = "/foo/bar";
    File fileFolder = new File(folder);

    FileSystem mockfs = EasyMock.createMock(FileSystem.class);
    expect(mockfs.exists(EasyMock.eq(fileFolder))).andReturn(true);
    expect(mockfs.isDirectory(EasyMock.eq(fileFolder))).andReturn(true);
    EasyMock.replay(mockfs);
    AppContext.RUN.fileSystem = mockfs;

    // Run the .getCodebasePath method.
    FileCodebaseCreator cc = new FileCodebaseCreator();
    File newPath = cc.getCodebasePath(fileFolder);
    assertEquals(newPath, fileFolder); // Should be the same for directories.
    EasyMock.verify(mockfs);
  }

  /**
   * Tests whether the getCodebasePath() method works with an unknown file type.
   * @throws CodebaseCreationError
   */
  public void testGetCodebasePathWithUnknownFile() throws CodebaseCreationError {
    // Set up mocks.
    AppContextForTesting.initForTest();
    String filePath = "/foo/bar.sth";
    File fileFolder = new File(filePath);

    FileSystem mockfs = EasyMock.createMock(FileSystem.class);
    expect(mockfs.exists(EasyMock.eq(fileFolder))).andReturn(true);
    expect(mockfs.isDirectory(EasyMock.eq(fileFolder))).andReturn(false);
    expect(mockfs.isFile(EasyMock.eq(fileFolder))).andReturn(true);
    EasyMock.replay(mockfs);
    AppContext.RUN.fileSystem = mockfs;

    // Run the .getCodebasePath method.
    FileCodebaseCreator cc = new FileCodebaseCreator();
    try {
      cc.getCodebasePath(fileFolder);
      fail("getCodebasePath() did not throw an exception for an unsupported file type.");
    } catch (CodebaseCreationError expected) {}
    EasyMock.verify(mockfs);
  }

  /**
   * Tests whether the getCodebasePath() method works with a .tar.
   */
  public void testGetCodebasePathWithKnownFile()
      throws CodebaseCreationError, IOException, CommandException {
    // Set up mocks.
    AppContextForTesting.initForTest();
    String filePath = "/foo/bar.tar";
    File fileFolder = new File(filePath);

    FileSystem mockfs = EasyMock.createMock(FileSystem.class);
    expect(mockfs.exists(EasyMock.eq(fileFolder))).andReturn(true);
    expect(mockfs.isDirectory(EasyMock.eq(fileFolder))).andReturn(false);
    expect(mockfs.isFile(EasyMock.eq(fileFolder))).andReturn(true);
    expect(mockfs.getTemporaryDirectory(EasyMock.<String>anyObject())).andReturn(new File("sth"));
    mockfs.makeDirs(EasyMock.<File>anyObject());
    EasyMock.expectLastCall().atLeastOnce();
    EasyMock.replay(mockfs);
    AppContext.RUN.fileSystem = mockfs;

    CommandRunner mockcmd = EasyMock.createMock(CommandRunner.class);
    expect(mockcmd.runCommand(EasyMock.<String>anyObject(),
                              EasyMock.<List<String>>anyObject(),
                              EasyMock.<String>anyObject(),
                              EasyMock.<String>anyObject())).andReturn(null);
    EasyMock.replay(mockcmd);
    AppContext.RUN.cmd = mockcmd;

    // Run the .getCodebasePath method.
    FileCodebaseCreator cc = new FileCodebaseCreator();
    File codebasePath = cc.getCodebasePath(fileFolder);
    assertNotNull(codebasePath);
    EasyMock.verify(mockfs);
    EasyMock.verify(mockcmd);
  }
}
