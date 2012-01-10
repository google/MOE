// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.database.DbStorage;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.database.SubmittedMigration;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;

import java.io.File;

/**
 */
public class BookkeepingDirectiveTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testHeadsEquivalent() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    AppContext.RUN.fileSystem = fileSystem;
    CommandRunner cmd = control.createMock(CommandRunner.class);
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
    BookkeepingDirective d = new BookkeepingDirective();
    File dbFile = new File("/path/to/db");
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = dbFile.getPath();

    expect(fileSystem.fileToString(dbFile)).andReturn("{\"equivalences\":[],\"migrations\":[]}");

    // updateCompletedMigrations
    // getRelativeFilenames for Codebase from(revision=1)>public
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.of(new File("/dummy/path/file")));
    // getRelativeFilenames for Codebase to(revision=migrate)
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.<File>of());
    // diffFiles
    // exists in from
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(true);
    // does not exist in to
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(false);
    // executable in from
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(true);
    // not executable in to
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(false);
    // diff
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/dummy/path/file", "/dummy/path/file"),
                          "")).andReturn("different");

    //updateHeadEquivalence
    // getRelativeFilenames for Codebase from
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.of(new File("/dummy/path/file")));
    // getRelativeFilenames for Codebase to
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.of(new File("/dummy/path/file")));

    // diffFiles
    // exists in from
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(true);
    // exists in to
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(true);
    // executable in from
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(true);
    // executable in to
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(true);
    // diff
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/dummy/path/file", "/dummy/path/file"),
                          "")).andReturn(null);

    // expected db at end of call to bookkeep
    DbStorage dbStorage = new DbStorage();
    dbStorage.addEquivalence(new Equivalence(new Revision("1", "int"), new Revision("1", "pub")));
    dbStorage.addMigration(
        new SubmittedMigration(new Revision("1", "int"), new Revision("migrate", "pub")));
    FileDb expectedDb = new FileDb(dbStorage);
    fileSystem.write(expectedDb.toJsonString(), dbFile);

    control.replay();
    assertEquals(0, d.perform());
    control.verify();
  }

  public void testOneSubmittedMigration() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    AppContext.RUN.fileSystem = fileSystem;
    CommandRunner cmd = control.createMock(CommandRunner.class);
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
    BookkeepingDirective d = new BookkeepingDirective();
    File dbFile = new File("/path/to/db");
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = dbFile.getPath();

    expect(fileSystem.fileToString(dbFile)).andReturn("{\"equivalences\":[],\"migrations\":[]}");
    // updateHeadEquivalence
    // getRelativeFilenames for Codebase from
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.of(new File("/dummy/path/file")));
    // getRelativeFilenames for Codebase to
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.<File>of());
    // diffFiles
    // exists in from
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(true);
    // does not exist in to
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(false);
    // executable in from
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(true);
    // not executable in to
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(false);
    // diff
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/dummy/path/file", "/dummy/path/file"),
                          "")).andReturn("different");

    // updateCompletedMigrations
    // getRelativeFilenames for Codebase from(revision=1)>public
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.of(new File("/dummy/path/file")));
    // getRelativeFilenames for Codebase to(revision=migrate)
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.<File>of());
    // diffFiles
    // exists in from
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(true);
    // does not exist in to
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(false);
    // executable in from
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(true);
    // not executable in to
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(false);
    // diff
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/dummy/path/file", "/dummy/path/file"),
                          "")).andReturn("different");

    // expected db at end of call to bookkeep
    DbStorage dbStorage = new DbStorage();
    dbStorage.addMigration(
        new SubmittedMigration(new Revision("1", "int"), new Revision("migrate", "pub")));
    FileDb expectedDb = new FileDb(dbStorage);
    fileSystem.write(expectedDb.toJsonString(), dbFile);

    control.replay();
    assertEquals(0, d.perform());
    control.verify();
  }

  public void testSubmittedMigrationHeadsEquivalent() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    AppContext.RUN.fileSystem = fileSystem;
    CommandRunner cmd = control.createMock(CommandRunner.class);
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
    BookkeepingDirective d = new BookkeepingDirective();
    File dbFile = new File("/path/to/db");
    d.getFlags().configFilename = "moe_config.txt";
    d.getFlags().dbLocation = dbFile.getPath();

    expect(fileSystem.fileToString(dbFile)).andReturn("{\"equivalences\":[],\"migrations\":[]}");
    // updateHeadEquivalence
    // getRelativeFilenames for Codebase from
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.of(new File("/dummy/path/file")));
    // getRelativeFilenames for Codebase to
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.<File>of());
    // diffFiles
    // exists in from
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(true);
    // does not exist in to
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(false);
    // executable in from
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(true);
    // not executable in to
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(false);
    // diff
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/dummy/path/file", "/dummy/path/file"),
                          "")).andReturn("different");

    // updateCompletedMigrations
    // getRelativeFilenames for Codebase from(revision=1)>public
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.of(new File("/dummy/path/file")));
    // getRelativeFilenames for Codebase to(revision=migrate)
    expect(fileSystem.findFiles(new File("/dummy/path"))).
        andReturn(ImmutableSet.<File>of());
    // diffFiles
    // exists in from
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(true);
    // exists in to
    expect(fileSystem.exists(new File("/dummy/path/file"))).andReturn(true);
    // executable in from
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(true);
    // executable in to
    expect(fileSystem.isExecutable(new File("/dummy/path/file"))).andReturn(true);
    // diff
    expect(cmd.runCommand("diff", ImmutableList.of("-N", "/dummy/path/file", "/dummy/path/file"),
                          "")).andReturn(null);

    // expected db at end of call to bookkeep
    DbStorage dbStorage = new DbStorage();
    dbStorage.addEquivalence(new Equivalence(new Revision("1", "int"), new Revision("1", "pub")));
    dbStorage.addMigration(
        new SubmittedMigration(new Revision("1", "int"), new Revision("migrate", "pub")));
    FileDb expectedDb = new FileDb(dbStorage);
    fileSystem.write(expectedDb.toJsonString(), dbFile);

    control.replay();
    assertEquals(0, d.perform());
    control.verify();
  }
}
