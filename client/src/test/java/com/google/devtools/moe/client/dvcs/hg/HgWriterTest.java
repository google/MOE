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

package com.google.devtools.moe.client.dvcs.hg;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.TestingModule;
import com.google.devtools.moe.client.writer.DraftRevision;
import dagger.Provides;
import java.io.File;
import javax.inject.Singleton;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.joda.time.DateTime;

// TODO(user): Create a FakeFileSystem to replace explicit mocked calls.
public class HgWriterTest extends TestCase {
  private static final File CODEBASE_ROOT = new File("/codebase");
  private static final File WRITER_ROOT = new File("/writer");
  private static final String PROJECT_SPACE = "public";
  private static final RepositoryExpression CODEBASE_EXPR =
      RepositoryExpression.create(PROJECT_SPACE);

  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFs = control.createMock(FileSystem.class);
  private final CommandRunner mockCmd = control.createMock(CommandRunner.class);
  private final RepositoryConfig mockRepoConfig = control.createMock(RepositoryConfig.class);
  private final Codebase codebase = Codebase.create(CODEBASE_ROOT, PROJECT_SPACE, CODEBASE_EXPR);
  private final HgClonedRepository mockRevClone = control.createMock(HgClonedRepository.class);

  /* Helper methods */

  private void expectHgCmd(String... args) throws CommandException {
    expect(mockRevClone.runHgCommand(WRITER_ROOT, ImmutableList.copyOf(args)))
        .andReturn("" /*stdout*/);
  }

  /* End helper methods */

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, Module.class})
  @Singleton
  interface Component {
    Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module
  class Module {
    @Provides
    public CommandRunner cmd() {
      return mockCmd;
    }

    @Provides
    public FileSystem filesystem() {
      return mockFs;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE =
        DaggerHgWriterTest_Component.builder().module(new Module()).build().context();

    expect(mockRevClone.getLocalTempDir()).andReturn(WRITER_ROOT).anyTimes();
    expect(mockRevClone.getConfig()).andReturn(mockRepoConfig).anyTimes();
    expect(mockRepoConfig.getIgnoreFilePatterns()).andReturn(ImmutableList.<String>of()).anyTimes();
    expect(mockRepoConfig.getProjectSpace()).andReturn(PROJECT_SPACE).anyTimes();
  }

  public void testPutCodebase_emptyCodebase() throws Exception {

    // Define the files in the codebase and in the writer (hg repo).
    expect(mockFs.findFiles(CODEBASE_ROOT)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(WRITER_ROOT))
        .andReturn(
            ImmutableSet.<File>of(
                new File(WRITER_ROOT, ".hg"),
                new File(WRITER_ROOT, ".hgignore"),
                new File(WRITER_ROOT, ".hg/branch"),
                new File(WRITER_ROOT, ".hg/cache/tags")));

    // Expect no other mockFs calls from HgWriter.putFile().

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    DraftRevision draftRevision = writer.putCodebase(codebase, null);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }

  public void testPutCodebase_addFile() throws Exception {

    expect(mockFs.findFiles(CODEBASE_ROOT))
        .andReturn(ImmutableSet.<File>of(new File(CODEBASE_ROOT, "file1")));
    expect(mockFs.findFiles(WRITER_ROOT)).andReturn(ImmutableSet.<File>of());

    expect(mockFs.exists(new File(CODEBASE_ROOT, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(WRITER_ROOT, "file1"))).andReturn(false);

    mockFs.makeDirsForFile(new File(WRITER_ROOT, "file1"));
    mockFs.copyFile(new File(CODEBASE_ROOT, "file1"), new File(WRITER_ROOT, "file1"));
    expectHgCmd("add", "file1");

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    DraftRevision draftRevision = writer.putCodebase(codebase, null);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }

  public void testPutCodebase_editFile() throws Exception {

    expect(mockFs.findFiles(CODEBASE_ROOT))
        .andReturn(ImmutableSet.<File>of(new File(CODEBASE_ROOT, "file1")));
    expect(mockFs.findFiles(WRITER_ROOT))
        .andReturn(ImmutableSet.<File>of(new File(WRITER_ROOT, "file1")));

    expect(mockFs.exists(new File(CODEBASE_ROOT, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(WRITER_ROOT, "file1"))).andReturn(true);

    mockFs.makeDirsForFile(new File(WRITER_ROOT, "file1"));
    mockFs.copyFile(new File(CODEBASE_ROOT, "file1"), new File(WRITER_ROOT, "file1"));

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    DraftRevision draftRevision = writer.putCodebase(codebase, null);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }

  public void testPutCodebase_removeFile() throws Exception {

    expect(mockFs.findFiles(CODEBASE_ROOT)).andReturn(ImmutableSet.<File>of());
    expect(mockFs.findFiles(WRITER_ROOT))
        .andReturn(ImmutableSet.<File>of(new File(WRITER_ROOT, "file1")));

    expect(mockFs.exists(new File(CODEBASE_ROOT, "file1"))).andReturn(false);
    expect(mockFs.exists(new File(WRITER_ROOT, "file1"))).andReturn(true);

    expectHgCmd("rm", "file1");

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    DraftRevision draftRevision = writer.putCodebase(codebase, null);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }

  public void testPutCodebase_editFileWithMetadata() throws Exception {
    expect(mockFs.findFiles(CODEBASE_ROOT))
        .andReturn(ImmutableSet.<File>of(new File(CODEBASE_ROOT, "file1")));
    expect(mockFs.findFiles(WRITER_ROOT))
        .andReturn(ImmutableSet.<File>of(new File(WRITER_ROOT, "file1")));

    expect(mockFs.exists(new File(CODEBASE_ROOT, "file1"))).andReturn(true);
    expect(mockFs.exists(new File(WRITER_ROOT, "file1"))).andReturn(true);

    mockFs.makeDirsForFile(new File(WRITER_ROOT, "file1"));
    mockFs.copyFile(new File(CODEBASE_ROOT, "file1"), new File(WRITER_ROOT, "file1"));

    expectHgCmd("status");

    control.replay();

    HgWriter writer = new HgWriter(mockRevClone);
    RevisionMetadata revisionMetadata =
        RevisionMetadata.builder()
            .id("rev1")
            .author("author")
            .date(new DateTime(1L))
            .description("desc")
            .build();
    DraftRevision draftRevision = writer.putCodebase(codebase, revisionMetadata);

    control.verify();

    assertEquals(WRITER_ROOT.getAbsolutePath(), draftRevision.getLocation());
  }
}
