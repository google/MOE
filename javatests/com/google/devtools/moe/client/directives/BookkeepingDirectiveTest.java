// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Moe;
import com.google.devtools.moe.client.database.DbStorage;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.database.SubmittedMigration;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.InMemoryFileSystem;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

import javax.inject.Singleton;

/**
 * Tests for {@link BookkeepingDirective}. DummyRepository is used, and per the bookkeeping flow,
 * dummy codebases are diffed at head (internal and public revision "1") and at one migrated
 * revision (internal "migrated_from" and public "migrated_to"). Note that equivalence of these
 * codebases -- /dummy/codebase/{int,pub}/1 and
 * /dummy/codebase/{int/migrated_from,pub/migrated_to} -- is implicitly determined by
 * existence/nonexistence of corresponding files in the {@code FileSystem} and <em>not</em> by the
 * output of the "diff" command, which is merely stubbed in.
 *
 */
public class BookkeepingDirectiveTest extends TestCase {

  private static final File DB_FILE = new File("/path/to/db");

  private final IMocksControl control = EasyMock.createControl();
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final InMemoryProjectContextFactory contextFactory = new InMemoryProjectContextFactory();

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {RecordingUi.Module.class, Module.class})
  @Singleton
  interface Component extends Moe.Component {
    @Override Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }


  @dagger.Module class Module {
    private final FileSystem fileSystem;
    Module(ImmutableMap<String, String> fileSystem) {
      this.fileSystem = new InMemoryFileSystem(fileSystem);
    }
    @Provides public ProjectContextFactory projectContextFactory() {
      return contextFactory;
    }
    @Provides public FileSystem fileSystem() {
      return fileSystem;
    }
    @Provides public CommandRunner commandRunner() {
      return cmd;
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\":\"foo\",\"repositories\":{" +
        "\"int\":{\"type\":\"dummy\",\"project_space\":\"internal\"}," +
        "\"pub\":{\"type\":\"dummy\"}}," +
        "\"translators\":[{\"from_project_space\":\"internal\"," +
        "\"to_project_space\":\"public\",\"steps\":[{\"name\":\"id_step\"," +
        "\"editor\":{\"type\":\"identity\"}}]}]," +
        "\"migrations\":[{\"name\":\"test\",\"from_repository\":\"int\"," +
        "\"to_repository\":\"pub\"}]}");
  }

  private void expectDiffs() throws Exception {
    // updateCompletedMigrations
    expect(cmd.runCommand(
        "diff",
        ImmutableList.of(
            "-N",
            "/dummy/codebase/int/migrated_from/file", "/dummy/codebase/pub/migrated_to/file"),
        "")).andReturn("unused");

    // updateHeadEquivalence
    expect(cmd.runCommand(
        "diff",
        ImmutableList.of("-N", "/dummy/codebase/int/1/file", "/dummy/codebase/pub/1/file"),
        "")).andReturn("unused");
  }

  /**
   * Bookkeeping for codebases the same at head, different at migrated revs.
   */
  public void testHeadsEquivalent() throws Exception {
    Injector.INSTANCE = DaggerBookkeepingDirectiveTest_Component.builder()
        .module(new Module(ImmutableMap.of(
            "/path/to/db", "{\"equivalences\":[], \"migrations\":[]}",
            "/dummy/codebase/int/1/file", "1",
            "/dummy/codebase/pub/1/file", "1 (equivalent)",
            "/dummy/codebase/int/migrated_from/file", "migrated_from",
            "/dummy/codebase/pub/migrated_to/", "dir (different)")))
        .build()
        .context();
    BookkeepingDirective d = new BookkeepingDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = DB_FILE.getAbsolutePath();

    expectDiffs();

    control.replay();
    assertEquals(0, d.perform());
    control.verify();

    // expected db at end of call to bookkeep
    DbStorage dbStorage = new DbStorage();
    dbStorage.addEquivalence(new Equivalence(new Revision("1", "int"), new Revision("1", "pub")));
    dbStorage.addMigration(new SubmittedMigration(
        new Revision("migrated_from", "int"), new Revision("migrated_to", "pub")));
    FileDb expectedDb = new FileDb(dbStorage);

    assertEquals(expectedDb.toJsonString(), Injector.INSTANCE.fileSystem.fileToString(DB_FILE));
  }

  /**
   * Bookkeeping for codebases different at head and migrated revs.
   */
  public void testOneSubmittedMigration_nonEquivalent() throws Exception {
    Injector.INSTANCE = DaggerBookkeepingDirectiveTest_Component.builder()
        .module(new Module(ImmutableMap.of(
            "/path/to/db", "{\"equivalences\":[], \"migrations\":[]}",
            "/dummy/codebase/int/1/file", "1",
            "/dummy/codebase/pub/1/", "empty dir (different)",
            "/dummy/codebase/int/migrated_from/file", "migrated_from",
            "/dummy/codebase/pub/migrated_to/", "empty dir (different)")))
        .build()
        .context();

    BookkeepingDirective d = new BookkeepingDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = DB_FILE.getAbsolutePath();

    expectDiffs();

    control.replay();
    assertEquals(0, d.perform());
    control.verify();

    // expected db at end of call to bookkeep
    DbStorage dbStorage = new DbStorage();
    dbStorage.addMigration(new SubmittedMigration(
        new Revision("migrated_from", "int"), new Revision("migrated_to", "pub")));
    FileDb expectedDb = new FileDb(dbStorage);

    assertEquals(expectedDb.toJsonString(), Injector.INSTANCE.fileSystem.fileToString(DB_FILE));
  }

  /**
   * Bookkeeping for codebases different at head and equivalent at migrated revs.
   */
  public void testOneSubmittedMigration_equivalent() throws Exception {
    Injector.INSTANCE = DaggerBookkeepingDirectiveTest_Component.builder()
        .module(new Module(ImmutableMap.of(
            "/path/to/db", "{\"equivalences\":[], \"migrations\":[]}",
            "/dummy/codebase/int/1/file", "1",
            "/dummy/codebase/pub/1/", "empty dir (different)",
            "/dummy/codebase/int/migrated_from/file", "migrated_from",
            "/dummy/codebase/pub/migrated_to/file", "migrated_to (equivalent)")))
        .build()
        .context();

    BookkeepingDirective d = new BookkeepingDirective();
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = DB_FILE.getAbsolutePath();

    expectDiffs();

    control.replay();
    assertEquals(0, d.perform());
    control.verify();

    // expected db at end of call to bookkeep
    DbStorage dbStorage = new DbStorage();
    dbStorage.addEquivalence(new Equivalence(
        new Revision("migrated_from", "int"), new Revision("migrated_to", "pub")));
    dbStorage.addMigration(new SubmittedMigration(
        new Revision("migrated_from", "int"), new Revision("migrated_to", "pub")));
    FileDb expectedDb = new FileDb(dbStorage);

    assertEquals(expectedDb.toJsonString(), Injector.INSTANCE.fileSystem.fileToString(DB_FILE));
  }
}
