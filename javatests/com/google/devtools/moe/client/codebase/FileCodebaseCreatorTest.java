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

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.testing.FileCodebaseCreator;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Tests for the FileCodebaseCreator class.
 */
public class FileCodebaseCreatorTest extends TestCase {
  private final RecordingUi ui = new RecordingUi();
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockfs = control.createMock(FileSystem.class);

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = new Injector(mockfs, cmd, ui);
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
    FileCodebaseCreator cc = new FileCodebaseCreator(mockfs);

    // Validate parameters.
    try {
      cc.create(ImmutableMap.<String, String>of());
      fail("Method does not check for required options.");
    } catch (CodebaseCreationError expected) {
    }

    try {
      cc.create(ImmutableMap.<String, String>of("path", "FooBar", "unknown", "123"));
      fail("Method does not check for unsupported options.");
    } catch (MoeProblem expected) {
    }
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
    FileCodebaseCreator cc = new FileCodebaseCreator(mockfs);
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
    FileCodebaseCreator cc = new FileCodebaseCreator(mockfs);
    Codebase codebase =
        cc.create(ImmutableMap.<String, String>of("path", folder, "projectspace", "internal"));
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
    } catch (CodebaseCreationError expected) {
    }
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
    expect(mockcmd.runCommand(
            EasyMock.<String>anyObject(),
            EasyMock.<List<String>>anyObject(),
            EasyMock.<String>anyObject()))
        .andReturn(null);
    EasyMock.replay(mockcmd);
    Injector.INSTANCE = new Injector(mockfs, mockcmd, ui);

    control.replay();
    File codebasePath = FileCodebaseCreator.getCodebasePath(fileFolder);
    control.verify();

    assertNotNull(codebasePath);
  }
}