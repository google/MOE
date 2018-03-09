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

package com.google.devtools.moe.client.dvcs.git;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.codebase.expressions.Term;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.testing.TestingModule;
import com.google.devtools.moe.client.writer.DraftRevision;
import dagger.Provides;
import java.io.File;
import javax.inject.Singleton;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

/**
 * Test GitWriter by expect()ing file system calls and git commands to add/remove files.
 */
public class GitWriterTest extends TestCase {

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFs = control.createMock(FileSystem.class);
  private final File codebaseRoot = new File("/codebase");
  private final File writerRoot = new File("/writer");
  private final String projectSpace = "public";
  private final RepositoryExpression cExp =
      new RepositoryExpression(new Term(projectSpace, ImmutableMap.<String, String>of()));
  private final Codebase codebase = Codebase.create(codebaseRoot, projectSpace, cExp);
  private final GitClonedRepository mockRevClone = control.createMock(GitClonedRepository.class);
  private final RepositoryConfig mockRepoConfig = control.createMock(RepositoryConfig.class);

  /* Helper methods */

  private void expectGitCmd(String... args) throws CommandException {
    expect(mockRevClone.runGitCommand(args)).andReturn("" /* stdout */);
  }

  /* End helper methods */

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, SystemCommandRunner.Module.class, Module.class})
  @Singleton
  interface Component {
    Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module
  class Module {
    @Provides
    public FileSystem filesystem() {
      return mockFs;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE =
        DaggerGitWriterTest_Component.builder().module(new Module()).build().context();

    expect(mockRevClone.getLocalTempDir()).andReturn(writerRoot).anyTimes();
    expect(mockRevClone.getConfig()).andReturn(mockRepoConfig).anyTimes();
    expect(mockRepoConfig.getProjectSpace()).andReturn(projectSpace).anyTimes();
  }

  public void testPutCodebase_emptyCodebase() throws Exception {
    expect(mockRepoConfig.getIgnoreFilePatterns()).andReturn(ImmutableList.<String>of());
    // Define the files in the codebase and in the writer (git repo).
    expect(mockFs.findFiles(codebaseRoot)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(writerRoot))
        .andReturn(
            ImmutableSet.<File>of(
                // Doesn't seem to matter that much what we return here, other than .git.
                new File(writerRoot, ".git/branches")));

    // Expect no other mockFs calls from GitWriter.putFile().

    control.replay();

    GitWriter w = new GitWriter(mockRevClone);
    DraftRevision dr = w.putCodebase(codebase, null);

    control.verify();

    assertEquals(writerRoot.getAbsolutePath(), dr.getLocation());
  }

  public void testPutCodebase_addFile() throws Exception {
    expect(mockRepoConfig.getIgnoreFilePatterns()).andReturn(ImmutableList.<String>of());

    expect(mockFs.findFiles(codebaseRoot))
        .andReturn(ImmutableSet.<File>of(new File(codebaseRoot, "file1")));
    expect(mockFs.findFiles(writerRoot)).andReturn(ImmutableSet.<File>of());

    expect(mockFs.exists(new File(codebaseRoot, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(writerRoot, "file1"))).andReturn(false);

    mockFs.makeDirsForFile(new File(writerRoot, "file1"));
    mockFs.copyFile(new File(codebaseRoot, "file1"), new File(writerRoot, "file1"));
    expectGitCmd("add", "-f", "file1");

    control.replay();

    GitWriter w = new GitWriter(mockRevClone);
    w.putCodebase(codebase, null);

    control.verify();
  }

  public void testPutCodebase_editFile() throws Exception {
    expect(mockRepoConfig.getIgnoreFilePatterns()).andReturn(ImmutableList.<String>of());

    expect(mockFs.findFiles(codebaseRoot))
        .andReturn(ImmutableSet.<File>of(new File(codebaseRoot, "file1")));
    expect(mockFs.findFiles(writerRoot))
        .andReturn(ImmutableSet.<File>of(new File(writerRoot, "file1")));

    expect(mockFs.exists(new File(codebaseRoot, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(writerRoot, "file1"))).andReturn(true);

    mockFs.makeDirsForFile(new File(writerRoot, "file1"));
    mockFs.copyFile(new File(codebaseRoot, "file1"), new File(writerRoot, "file1"));
    expectGitCmd("add", "-f", "file1");

    control.replay();

    GitWriter w = new GitWriter(mockRevClone);
    w.putCodebase(codebase, null);

    control.verify();
  }

  public void testPutCodebase_removeFile() throws Exception {
    expect(mockRepoConfig.getIgnoreFilePatterns()).andReturn(ImmutableList.<String>of());

    expect(mockFs.findFiles(codebaseRoot)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(writerRoot))
        .andReturn(ImmutableSet.<File>of(new File(writerRoot, "file1")));

    expect(mockFs.exists(new File(codebaseRoot, "file1"))).andReturn(false);
    expect(mockFs.exists(new File(writerRoot, "file1"))).andReturn(true);

    expectGitCmd("rm", "file1");

    control.replay();

    GitWriter w = new GitWriter(mockRevClone);
    w.putCodebase(codebase, null);

    control.verify();
  }

  public void testPutCodebase_ignoreFilesRes() throws Exception {
    expect(mockRepoConfig.getIgnoreFilePatterns())
        .andReturn(ImmutableList.of("^.*ignored_\\w+\\.txt$"));

    expect(mockFs.findFiles(codebaseRoot)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(writerRoot))
        .andReturn(
            ImmutableSet.<File>of(
                new File(writerRoot, ".git/branches"),
                new File(writerRoot, "not_really_ignored_dir/file1"),
                new File(writerRoot, "included_dir/ignored_file.txt")));

    expect(mockFs.exists(new File(codebaseRoot, "not_really_ignored_dir/file1"))).andReturn(false);
    expect(mockFs.exists(new File(writerRoot, "not_really_ignored_dir/file1"))).andReturn(true);

    expectGitCmd("rm", "not_really_ignored_dir/file1");

    control.replay();

    GitWriter w = new GitWriter(mockRevClone);
    DraftRevision dr = w.putCodebase(codebase, null);

    control.verify();

    assertEquals(writerRoot.getAbsolutePath(), dr.getLocation());
  }
}
