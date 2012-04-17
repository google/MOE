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
import com.google.devtools.moe.client.database.EquivalenceMatcher.EquivalenceMatchResult;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.AppContextForTesting;
import com.google.devtools.moe.client.testing.DummyDb;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;
import java.util.List;

/**
 * Unit tests for HgRevisionHistory: that 'hg log' calls are as expected and parsed correctly.
 *
 */
public class HgRevisionHistoryTest extends TestCase {

  private static final String MOCK_REPO_NAME = "mockrepo";
  private static final String CLONE_TEMP_DIR = "/tmp/hg_tipclone_mockrepo_12345";

  private IMocksControl control;
  private CommandRunner cmd;
  private RepositoryConfig config;

  @Override protected void setUp() throws Exception {
    super.setUp();
    AppContextForTesting.initForTest();
    control = EasyMock.createControl();
    cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;
  }

  private HgClonedRepository mockClonedRepo(String repoName) {
    HgClonedRepository mockRepo = control.createMock(HgClonedRepository.class);
    expect(mockRepo.getRepositoryName()).andReturn(repoName).anyTimes();
    expect(mockRepo.getLocalTempDir()).andReturn(new File(CLONE_TEMP_DIR)).anyTimes();

    config = control.createMock(RepositoryConfig.class);
    expect(mockRepo.getConfig()).andReturn(config).anyTimes();
    return mockRepo;
  }

  public void testFindHighestRevision() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "log",
                "--branch=" + HgRevisionHistory.DEFAULT_BRANCH,
                "--limit=1",
                "--template={node}"),
            CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("mockChangesetID");

    control.replay();

    HgRevisionHistory revHistory = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    Revision rev = revHistory.findHighestRevision(null);
    assertEquals(MOCK_REPO_NAME, rev.repositoryName);
    assertEquals("mockChangesetID", rev.revId);

    control.verify();
  }

  public void testFindHighestRevision_nonExistentChangesetThrows() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "log",
                "--branch=default",
                "--limit=1",
                "--template={node}",
                "--rev=bogusChangeset"),
            CLONE_TEMP_DIR /*workingDirectory*/))
        .andThrow(
            new CommandException(
                "hg",
                ImmutableList.<String>of("mock args"),
                "mock stdout",
                "mock stderr: unknown revision",
                255 /* Hg error code for unknown rev */));

    // Run test
    control.replay();

    try {
      HgRevisionHistory revHistory = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
      Revision rev = revHistory.findHighestRevision("bogusChangeset");
      fail("'hg log' didn't fail on bogus changeset ID");
    } catch (MoeProblem expected) {}

    control.verify();
  }

  public void testGetMetadata() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);

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
            CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("2 < uid@google.com < date < description < 1:parent1 2:parent2");

    control.replay();

    HgRevisionHistory revHistory = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    RevisionMetadata result = revHistory.getMetadata(new Revision("2", "mockrepo"));
    assertEquals("2", result.id);
    assertEquals("uid@google.com", result.author);
    assertEquals("date", result.date);
    assertEquals("description", result.description);
    assertEquals(ImmutableList.of(new Revision("parent1", MOCK_REPO_NAME),
                                  new Revision("parent2", MOCK_REPO_NAME)),
                 result.parents);

    control.verify();
  }

  public void testGetEscapedMetadata() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);

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
            CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("2 < u&lt;id@google.com < &amp;amp < &gt;description < 1:parent");

    control.replay();

    HgRevisionHistory revHistory = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    RevisionMetadata result = revHistory.getMetadata(new Revision("2", "mockrepo"));
    assertEquals("2", result.id);
    assertEquals("u<id@google.com", result.author);
    assertEquals("&amp", result.date);
    assertEquals(">description", result.description);
    assertEquals(ImmutableList.of(new Revision("parent", MOCK_REPO_NAME)), result.parents);

    control.verify();
  }

  public void testParseMetadata() throws Exception {
    HgRevisionHistory rh =
        new HgRevisionHistory(Suppliers.ofInstance(mockClonedRepo(MOCK_REPO_NAME)));

    control.replay();

    RevisionMetadata rm = rh.parseMetadata(
        "1 < foo@google.com < date1 < foo < 1:p1 -1:p2\n");
    assertEquals("1", rm.id);
    assertEquals("foo@google.com", rm.author);
    assertEquals("date1", rm.date);
    assertEquals("foo", rm.description);
    assertEquals(ImmutableList.of(new Revision("p1", MOCK_REPO_NAME)),
                 rm.parents);

    control.verify();
  }

  public void testFindHeadRevisions() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);
    expect(config.getImportBranches()).andReturn(ImmutableList.of("branch1", "branch2", "unused"));

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "heads",
                "--template={node} {branch}\n"),
            CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("mockChangesetID1 branch1\nmockChangesetID2 branch2\nmockChangesetID3 unused");

    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    ImmutableList<Revision> revs = ImmutableList.copyOf(rh.findHeadRevisions());
    assertEquals(MOCK_REPO_NAME, revs.get(0).repositoryName);
    assertEquals("mockChangesetID1", revs.get(0).revId);
    assertEquals(MOCK_REPO_NAME, revs.get(1).repositoryName);
    assertEquals("mockChangesetID2", revs.get(1).revId);

    control.verify();
  }

  public void testFindNewRevisions() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);
    expect(config.getImportBranches()).andReturn(null);
    DummyDb db = new DummyDb(false);

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "heads",
                "--template={node} {branch}\n"),
            CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("mockChangesetID default\n");

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
            CLONE_TEMP_DIR /*workingDirectory*/))
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
            CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("parent < uid@google.com < date < description < ");

    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    List<Revision> newRevisions = rh.findRevisions(null, new EquivalenceMatcher("public", db))
        .getRevisionsSinceEquivalence()
        .getLinearHistory();
    assertEquals(2, newRevisions.size());
    assertEquals(MOCK_REPO_NAME, newRevisions.get(0).repositoryName);
    assertEquals("mockChangesetID", newRevisions.get(0).revId);
    assertEquals(MOCK_REPO_NAME, newRevisions.get(1).repositoryName);
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
    expect(config.getImportBranches()).andReturn(null);

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "heads",
                "--template={node} {branch}\n"),
            CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("4 default\n");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=4", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("4 < author < date < description < par1:3a par2:3b");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=3a", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("3a < author < date < description < par1:2 -1:0");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=3b", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("3b < author < date < description < par1:2 -1:0");

    control.replay();

    FileDb database = FileDb.makeDbFromDbText(testDb1);

    HgRevisionHistory history = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));

    EquivalenceMatchResult result =
        history.findRevisions(null, new EquivalenceMatcher("repo1", database));

    Equivalence expectedEq = new Equivalence(new Revision("1002", "repo1"),
                                             new Revision("2", "repo2"));
    assertEquals(ImmutableList.of(expectedEq), result.getEquivalences());

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
        CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("4 < author < date < description < par1:3a par2:3b");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=3a", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("3a < author < date < description < par1:2 -1:0");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=3b", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("3b < author < date < description < par1:2 -1:0");

    expect(cmd.runCommand("hg", ImmutableList.of("log", "--rev=2", "--limit=1",
        "--template={node|escape} < {author|escape} < {date|date|escape} < " +
        "{desc|escape} < {parents|stringify|escape}", "--debug"),
        CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("2 < author < date < description < -1:0 -1:0");

    control.replay();

    FileDb database = FileDb.makeDbFromDbText(testDb2);

    HgRevisionHistory history = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    EquivalenceMatchResult result = history.findRevisions(
        new Revision("4", "repo2"), new EquivalenceMatcher("repo1", database));

    assertEquals(0, result.getEquivalences().size());

    control.verify();
  }
}
