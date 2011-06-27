// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.List;

/**
 *
 */
public class HgRevisionHistoryTest extends TestCase {

  private IMocksControl control;
  private CommandRunner cmd;
  private String repositoryName = "mockrepo";
  private String localCloneTempDir = "/tmp/hg_clone_mockrepo_12345";

  @Override public void setUp() {
    AppContextForTesting.initForTest();
    control = EasyMock.createControl();
    cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
  }

  @Override public void tearDown() {
    control = null;
    cmd = null;
    AppContext.RUN.cmd = null;
  }

  public void testFindHighestRevision() {
    // Mock cloned repo
    HgClonedRepository mockRepo = control.createMock(HgClonedRepository.class);
    expect(mockRepo.getLocalTempDir()).andReturn(new File(localCloneTempDir));
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);

    // Mock 'hg log' command
    try {
      expect(
          cmd.runCommand(
              "hg",
              ImmutableList.<String>of(
                  "log",
                  "--rev=tip",
                  "--limit=1",
                  "--template='{node}'",
                  localCloneTempDir),
              "" /*stdinData*/,
              "" /*workingDirectory*/))
          .andReturn("mockChangesetID");
    } catch (CommandException e) { throw new RuntimeException(e); }

    // Run test
    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(mockRepo);
    Revision rev = rh.findHighestRevision(null);
    assertEquals(repositoryName, rev.repositoryName);
    assertEquals("mockChangesetID", rev.revId);

    control.verify();
  }

  public void testFindHighestRevision_nonExistentChangesetThrows() {
    // Mock cloned repo
    HgClonedRepository mockRepo = control.createMock(HgClonedRepository.class);
    expect(mockRepo.getLocalTempDir()).andReturn(new File(localCloneTempDir));

    // Mock 'hg log' command
    try {
      expect(
          cmd.runCommand(
              "hg",
              ImmutableList.<String>of(
                  "log",
                  "--rev=bogusChangeset",
                  "--limit=1",
                  "--template='{node}'",
                  localCloneTempDir),
              "" /*stdinData*/,
              "" /*workingDirectory*/))
          .andThrow(
              new CommandException(
                  "hg",
                  ImmutableList.<String>of("mock args"),
                  "mock stdout",
                  "mock stderr: unknown revision",
                  255 /* Hg error code for unknown rev */));
    } catch (CommandException e) { throw new RuntimeException(e); }

    // Run test
    control.replay();

    try {
      HgRevisionHistory rh = new HgRevisionHistory(mockRepo);
      Revision rev = rh.findHighestRevision("bogusChangeset");
      fail("'hg log' didn't fail on bogus changeset ID");
    } catch (MoeProblem expected) {}

    control.verify();
  }

  public void testGetMetadata() {
    // Mock cloned repo
    HgClonedRepository mockRepo = control.createMock(HgClonedRepository.class);
    expect(mockRepo.getLocalTempDir()).andReturn(new File(localCloneTempDir));
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);

    // Mock 'hg log' command
    try {
      expect(
          cmd.runCommand(
              "hg",
              ImmutableList.<String>of(
                  "log",
                  "--rev=2",
                  "--limit=1",
                  "--template='{node|escape} < {author|escape} < " +
                              "{date|date|escape} < {desc|escape} < " +
                              "{parents|escape}'",
                  "--debug",
                  localCloneTempDir),
              "" /*stdinData*/,
              "" /*workingDirectory*/))
          .andReturn("2 < uid@google.com < date < description < parent1 parent2");
    } catch (CommandException e) { throw new RuntimeException(e); }
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
    // Run test
    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(mockRepo);
    RevisionMetadata result = rh.getMetadata(new Revision("2", "mockrepo"));
    assertEquals("2", result.id);
    assertEquals("uid@google.com", result.author);
    assertEquals("date", result.date);
    assertEquals("description", result.description);
    assertEquals(ImmutableList.of(new Revision("parent1", repositoryName),
                                  new Revision("parent2", repositoryName)),
                 result.parents);
    control.verify();
  }

  public void testGetEscapedMetadata() {
    // Mock cloned repo
    HgClonedRepository mockRepo = control.createMock(HgClonedRepository.class);
    expect(mockRepo.getLocalTempDir()).andReturn(new File(localCloneTempDir));
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);

    // Mock 'hg log' command
    try {
      expect(
          cmd.runCommand(
              "hg",
              ImmutableList.<String>of(
                  "log",
                  "--rev=2",
                  "--limit=1",
                  "--template='{node|escape} < {author|escape} < " +
                              "{date|date|escape} < {desc|escape} < " +
                              "{parents|escape}'",
                  "--debug",
                  localCloneTempDir),
              "" /*stdinData*/,
              "" /*workingDirectory*/))
          .andReturn("2 < u&ltid@google.com < &ampamp < &gtdescription < parent");
    } catch (CommandException e) { throw new RuntimeException(e); }
    expect(mockRepo.getRepositoryName()).andReturn(repositoryName);
    // Run test
    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(mockRepo);
    RevisionMetadata result = rh.getMetadata(new Revision("2", "mockrepo"));
    assertEquals("2", result.id);
    assertEquals("u<id@google.com", result.author);
    assertEquals("&amp", result.date);
    assertEquals(">description", result.description);
    assertEquals(ImmutableList.of(new Revision("parent", repositoryName)), result.parents);
    control.verify();
  }

  public void testParseMetadata() {
    HgRevisionHistory rh = new HgRevisionHistory(new HgClonedRepository(repositoryName,
                                                                        localCloneTempDir));
    List<RevisionMetadata> rs = rh.parseMetadata(
        "1 < foo@google.com < date1 < foo < p1 p2\n" +
        "2 < bar@google.com < date2 < bar < ");
    assertEquals(2, rs.size());
    assertEquals("1", rs.get(0).id);
    assertEquals("foo@google.com", rs.get(0).author);
    assertEquals("date1", rs.get(0).date);
    assertEquals("foo", rs.get(0).description);
    assertEquals(ImmutableList.of(new Revision("p1", repositoryName),
                                  new Revision("p2", repositoryName)),
                 rs.get(0).parents);
    assertEquals("2", rs.get(1).id);
    assertEquals("bar@google.com", rs.get(1).author);
    assertEquals("date2", rs.get(1).date);
    assertEquals("bar", rs.get(1).description);
    assertEquals(ImmutableList.<Revision>of(), rs.get(1).parents);
  }
}
