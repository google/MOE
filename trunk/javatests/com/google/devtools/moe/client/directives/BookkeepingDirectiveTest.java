// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.database.DbStorage;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.database.SubmittedMigration;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryFileSystem;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

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

  private IMocksControl control;
  private CommandRunner cmd;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    AppContextForTesting.initForTest();
    control = EasyMock.createControl();
    cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;

    // This MOE config contains:
    //  - dummy internal and public repositories (int and pub, respectively)
    //  - a translator from internal to public consisting of a single identity step
    //  - a migration named 'test' from int to pub with no additional config info
    ((InMemoryProjectContextFactory) AppContext.RUN.contextFactory).projectConfigs.put(
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
    InMemoryFileSystem fileSystem = new InMemoryFileSystem(ImmutableMap.of(
        "/path/to/db", "{\"equivalences\":[], \"migrations\":[]}",
        "/dummy/codebase/int/1/file", "1",
        "/dummy/codebase/pub/1/file", "1 (equivalent)",
        "/dummy/codebase/int/migrated_from/file", "migrated_from",
        "/dummy/codebase/pub/migrated_to/", "dir (different)"
        ));
    AppContext.RUN.fileSystem = fileSystem;

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

    assertEquals(expectedDb.toJsonString(), fileSystem.fileToString(DB_FILE));
  }

  /**
   * Bookkeeping for codebases different at head and migrated revs.
   */
  public void testOneSubmittedMigration_nonEquivalent() throws Exception {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem(ImmutableMap.of(
        "/path/to/db", "{\"equivalences\":[], \"migrations\":[]}",
        "/dummy/codebase/int/1/file", "1",
        "/dummy/codebase/pub/1/", "empty dir (different)",
        "/dummy/codebase/int/migrated_from/file", "migrated_from",
        "/dummy/codebase/pub/migrated_to/", "empty dir (different)"
        ));
    AppContext.RUN.fileSystem = fileSystem;

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

    assertEquals(expectedDb.toJsonString(), fileSystem.fileToString(DB_FILE));
  }

  /**
   * Bookkeeping for codebases different at head and equivalent at migrated revs.
   */
  public void testOneSubmittedMigration_equivalent() throws Exception {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem(ImmutableMap.of(
        "/path/to/db", "{\"equivalences\":[], \"migrations\":[]}",
        "/dummy/codebase/int/1/file", "1",
        "/dummy/codebase/pub/1/", "empty dir (different)",
        "/dummy/codebase/int/migrated_from/file", "migrated_from",
        "/dummy/codebase/pub/migrated_to/file", "migrated_to (equivalent)"
        ));
    AppContext.RUN.fileSystem = fileSystem;

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

    assertEquals(expectedDb.toJsonString(), fileSystem.fileToString(DB_FILE));
  }
}
