// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMatcher;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.DummyDb;

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
  private final String repositoryName = "mockrepo";
  private final String localCloneTempDir = "/tmp/hg_tipclone_mockrepo_12345";

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

  private HgClonedRepository mockClonedRepo(String repoName) {
    HgClonedRepository mockRepo = control.createMock(HgClonedRepository.class);
    expect(mockRepo.getRepositoryName()).andReturn(repoName).anyTimes();
    expect(mockRepo.getLocalTempDir()).andReturn(new File(localCloneTempDir)).anyTimes();
    return mockRepo;
  }

  public void testFindHighestRevision() {
    HgClonedRepository mockRepo = mockClonedRepo(repositoryName);

    try {
      expect(
          cmd.runCommand(
              "hg",
              ImmutableList.<String>of(
                  "log",
                  "--rev=tip",
                  "--limit=1",
                  "--template={node}"),
              localCloneTempDir /*workingDirectory*/))
          .andReturn("mockChangesetID");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    Revision rev = rh.findHighestRevision(null);
    assertEquals(repositoryName, rev.repositoryName);
    assertEquals("mockChangesetID", rev.revId);

    control.verify();
  }

  public void testFindHighestRevision_nonExistentChangesetThrows() {
    HgClonedRepository mockRepo = mockClonedRepo(repositoryName);

    try {
      expect(
          cmd.runCommand(
              "hg",
              ImmutableList.<String>of(
                  "log",
                  "--rev=bogusChangeset",
                  "--limit=1",
                  "--template={node}"),
              localCloneTempDir /*workingDirectory*/))
          .andThrow(
              new CommandException(
                  "hg",
                  ImmutableList.<String>of("mock args"),
                  "mock stdout",
                  "mock stderr: unknown revision",
                  255 /* Hg error code for unknown rev */));
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    // Run test
    control.replay();

    try {
      HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
      Revision rev = rh.findHighestRevision("bogusChangeset");
      fail("'hg log' didn't fail on bogus changeset ID");
    } catch (MoeProblem expected) {}

    control.verify();
  }

  public void testGetMetadata() {
    HgClonedRepository mockRepo = mockClonedRepo(repositoryName);

    try {
      expect(
          cmd.runCommand(
              "hg",
              ImmutableList.<String>of(
                  "log",
                  "--rev=2",
                  "--limit=1",
                  "--template={node|escape} < {author|escape} < " +
                              "{date|date|escape} < {desc|escape} < " +
                              "{parents|stringify|escape}",
                  "--debug"),
              localCloneTempDir /*workingDirectory*/))
          .andReturn("2 < uid@google.com < date < description < 1:parent1 2:parent2");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
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
    HgClonedRepository mockRepo = mockClonedRepo(repositoryName);

    try {
      expect(
          cmd.runCommand(
              "hg",
              ImmutableList.<String>of(
                  "log",
                  "--rev=2",
                  "--limit=1",
                  "--template={node|escape} < {author|escape} < " +
                              "{date|date|escape} < {desc|escape} < " +
                              "{parents|stringify|escape}",
                  "--debug"),
              localCloneTempDir /*workingDirectory*/))
          .andReturn("2 < u&lt;id@google.com < &amp;amp < &gt;description < 1:parent");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    RevisionMetadata result = rh.getMetadata(new Revision("2", "mockrepo"));
    assertEquals("2", result.id);
    assertEquals("u<id@google.com", result.author);
    assertEquals("&amp", result.date);
    assertEquals(">description", result.description);
    assertEquals(ImmutableList.of(new Revision("parent", repositoryName)), result.parents);
    control.verify();
  }

  public void testParseMetadata() {
    HgRevisionHistory rh =
        new HgRevisionHistory(Suppliers.ofInstance(mockClonedRepo(repositoryName)));
    control.replay();
    List<RevisionMetadata> rs = rh.parseMetadata(
        "1 < foo@google.com < date1 < foo < 1:p1 -1:p2\n");
    assertEquals(1, rs.size());
    assertEquals("1", rs.get(0).id);
    assertEquals("foo@google.com", rs.get(0).author);
    assertEquals("date1", rs.get(0).date);
    assertEquals("foo", rs.get(0).description);
    assertEquals(ImmutableList.of(new Revision("p1", repositoryName)),
                 rs.get(0).parents);
    control.verify();
  }

  public void testFindHeadRevisions() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(repositoryName);

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "heads",
                "--template={node}\n"),
            localCloneTempDir /*workingDirectory*/))
        .andReturn("mockChangesetID1\nmockChangesetID2\n");

    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    ImmutableList<Revision> revs = ImmutableList.copyOf(rh.findHeadRevisions());
    assertEquals(repositoryName, revs.get(0).repositoryName);
    assertEquals("mockChangesetID1", revs.get(0).revId);
    assertEquals(repositoryName, revs.get(1).repositoryName);
    assertEquals("mockChangesetID2", revs.get(1).revId);

    control.verify();
  }

  public void testFindNewRevisions() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(repositoryName);
    DummyDb db = new DummyDb(false);

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "heads",
                "--template={node}\n"),
            localCloneTempDir /*workingDirectory*/))
        .andReturn("mockChangesetID\n");

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "log",
                "--rev=mockChangesetID",
                "--limit=1",
                "--template={node|escape} < {author|escape} < " +
                            "{date|date|escape} < {desc|escape} < " +
                            "{parents|stringify|escape}",
                "--debug"),
            localCloneTempDir /*workingDirectory*/))
        .andReturn("mockChangesetID < uid@google.com < date < description < 1:parent");

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "log",
                "--rev=parent",
                "--limit=1",
                "--template={node|escape} < {author|escape} < " +
                            "{date|date|escape} < {desc|escape} < " +
                            "{parents|stringify|escape}",
                "--debug"),
            localCloneTempDir /*workingDirectory*/))
        .andReturn("parent < uid@google.com < date < description < ");

    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    RevisionMatcher matcher = new EquivalenceMatcher("public", db);
    ImmutableList<Revision> newRevisions = ImmutableList.copyOf(rh.findRevisions(null, matcher));
    assertEquals(2, newRevisions.size());
    assertEquals(repositoryName, newRevisions.get(0).repositoryName);
    assertEquals("mockChangesetID", newRevisions.get(0).revId);
    assertEquals(repositoryName, newRevisions.get(1).repositoryName);
    assertEquals("parent", newRevisions.get(1).revId);

    control.verify();
  }

  /*
   * A database that holds the following equivalences:
   * repo1{1002} == repo2{2}
   */
  private final String testDb1 = "{\"equivalences\":["
      + "{\"rev1\": {\"revId\":\"1002\",\"repositoryName\":\"repo1\"},"
      + " \"rev2\": {\"revId\":\"2\",\"repositoryName\":\"repo2\"}}]}";

  /**
   * A test for finding the last equivalence for the following history starting
   * with repo2{4}:
   *                                         _____
   *                                        |     |
   *                                        |  4  |
   *                                        |_____|
   *                                           |  \
   *                                           |   \
   *                                           |    \
   *                                         __|__   \_____
   *                                        |     |  |     |
   *                                        |  3a |  | 3b  |
   *                                        |_____|  |_____|
   *                                           |     /
   *                                           |    /
   *                                           |   /
   *              ____                       __|__/
   *             |    |                     |     |
   *             |1002|=====================|  2  |
   *             |____|                     |_____|
   *
   *              repo1                      repo2
   *
   * @throws Exception
   */
  public void testFindLastEquivalence() throws Exception {
    // Mock cloned repo
    HgClonedRepository mockRepo = mockClonedRepo("repo2");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=4", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        localCloneTempDir /*workingDirectory*/))
        .andReturn("4 < author < date < description < par1:3a par2:3b");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=3a", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        localCloneTempDir /*workingDirectory*/))
        .andReturn("3a < author < date < description < par1:2 -1:0");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=3b", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        localCloneTempDir /*workingDirectory*/))
        .andReturn("3b < author < date < description < par1:2 -1:0");

    control.replay();

    FileDb database = FileDb.makeDbFromDbText(testDb1);
    EquivalenceMatcher matcher = new EquivalenceMatcher("repo1", database);

    HgRevisionHistory history = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));

    Equivalence actualEq = history.findLastEquivalence(new Revision("4", "repo2"), matcher);
    Equivalence expectedEq = new Equivalence(new Revision("1002", "repo1"),
                                             new Revision("2", "repo2"));
    assertEquals(expectedEq, actualEq);

    control.verify();
  }

  /*
   * A database that holds the following equivalences:
   * repo1{1005} == repo2{5}
   */
  private final String testDb2 = "{\"equivalences\":["
      + "{\"rev1\": {\"revId\":\"1005\",\"repositoryName\":\"repo1\"},"
      + " \"rev2\": {\"revId\":\"5\",\"repositoryName\":\"repo2\"}}]}";

  /**
   * A test for finding the last equivalence for the following history starting
   * with repo2{4}:
   *
   *              ____                       _____
   *             |    |                     |     |
   *             |1005|=====================|  5  |
   *             |____|                     |_____|
   *                                           |
   *                                           |
   *                                           |
   *                                         __|__
   *                                        |     |
   *                                        |  4  |
   *                                        |_____|
   *                                           |  \
   *                                           |   \
   *                                           |    \
   *                                         __|__   \_____
   *                                        |     |  |     |
   *                                        |  3a |  | 3b  |
   *                                        |_____|  |_____|
   *                                           |     /
   *                                           |    /
   *                                           |   /
   *                                         __|__/
   *                                        |     |
   *                                        |  2  |
   *                                        |_____|
   *
   *              repo1                      repo2
   *
   * @throws Exception
   */
  public void testFindLastEquivalenceNull() throws Exception {
    // Mock cloned repo
    HgClonedRepository mockRepo = mockClonedRepo("repo2");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=4", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        localCloneTempDir /*workingDirectory*/))
        .andReturn("4 < author < date < description < par1:3a par2:3b");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=3a", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        localCloneTempDir /*workingDirectory*/))
        .andReturn("3a < author < date < description < par1:2 -1:0");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=3b", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        localCloneTempDir /*workingDirectory*/))
        .andReturn("3b < author < date < description < par1:2 -1:0");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=2", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        localCloneTempDir /*workingDirectory*/))
        .andReturn("2 < author < date < description < -1:0 -1:0");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=2", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        localCloneTempDir /*workingDirectory*/))
        .andReturn("2 < author < date < description < -1:0 -1:0");

    control.replay();

    FileDb database = FileDb.makeDbFromDbText(testDb2);
    EquivalenceMatcher matcher = new EquivalenceMatcher("repo1", database);

    HgRevisionHistory history = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));

    assertNull(history.findLastEquivalence(new Revision("4", "repo2"), matcher));

    control.verify();
  }
}
