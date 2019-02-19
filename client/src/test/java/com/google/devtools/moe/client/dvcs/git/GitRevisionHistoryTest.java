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

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.moe.client.repositories.RevisionHistory.SearchType.BRANCHED;
import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.DbStorage;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher.Result;
import com.google.devtools.moe.client.GsonModule;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.DummyDb;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;
import org.easymock.IMocksControl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Unit tests for GitRevisionHistory.
 */
public class GitRevisionHistoryTest extends TestCase {
  private static final String GIT_COMMIT_DATE = "2012-07-09 06:00:00 -0700";
  private static final DateTime DATE =
      // 2012/7/9, 6am
      new DateTime(2012, 7, 9, 6, 0, DateTimeZone.forOffsetHours(-7));

  private static final String LOG_FORMAT_COMMIT_ID = "%H";
  private static final Joiner METADATA_JOINER = Joiner.on(GitRevisionHistory.LOG_DELIMITER);
  private static final String LOG_FORMAT_ALL_METADATA =
      METADATA_JOINER.join("%H", "%an", "%ai", "%P", "%B");

  private final IMocksControl control = EasyMock.createControl();
  private final String repositoryName = "mockrepo";
  private final String localCloneTempDir = "/tmp/git_tipclone_mockrepo_12345";

  private GitClonedRepository mockClonedRepo(String repoName) throws CommandException {
    GitClonedRepository mockRepo = control.createMock(GitClonedRepository.class);

    RepositoryConfig repositoryConfig = control.createMock(RepositoryConfig.class);
    expect(repositoryConfig.getUrl()).andReturn(localCloneTempDir).anyTimes();

    expect(mockRepo.getRepositoryName()).andReturn(repoName).anyTimes();
    expect(mockRepo.getConfig()).andReturn(repositoryConfig).anyTimes();
    return mockRepo;
  }

  private IExpectationSetters<String> expectLogCommandIgnoringMissing(
      GitClonedRepository mockRepo, String logFormat, String revName) throws CommandException {
    return expect(
        mockRepo.runGitCommand(
            "log", "--max-count=1", "--format=" + logFormat, "--ignore-missing", revName));
  }

  private IExpectationSetters<String> expectLogCommand(
      GitClonedRepository mockRepo, String logFormat, String revName) throws CommandException {
    return expect(
        mockRepo.runGitCommand("log", "--max-count=1", "--format=" + logFormat, revName, "--"));
  }

  public void testFindHighestRevision() throws Exception {
    GitClonedRepository mockRepo = mockClonedRepo(repositoryName);

    expectLogCommand(mockRepo, LOG_FORMAT_COMMIT_ID, "HEAD").andReturn("mockHashID");

    control.replay();

    GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
    Revision rev = rh.findHighestRevision(null);
    assertEquals(repositoryName, rev.repositoryName());
    assertEquals("mockHashID", rev.revId());

    control.verify();
  }

  public void testFindHighestRevision_nonExistentHashThrows() throws Exception {
    GitClonedRepository mockRepo = mockClonedRepo(repositoryName);

    expectLogCommand(mockRepo, LOG_FORMAT_COMMIT_ID, "bogusHash")
        .andThrow(
            new CommandException(
                "git",
                ImmutableList.<String>of("mock args"),
                "mock stdout",
                "mock stderr: unknown revision",
                1));

    control.replay();

    try {
      GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
      rh.findHighestRevision("bogusHash");
      fail("'git log' didn't fail on bogus hash ID");
    } catch (MoeProblem expected) {
    }

    control.verify();
  }

  public void testGetMetadata() throws Exception {
    GitClonedRepository mockRepo = mockClonedRepo(repositoryName);

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "1")
        .andReturn(
            METADATA_JOINER.join("1", "foo@google.com", GIT_COMMIT_DATE, "2 3", "description\n"));

    control.replay();

    GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
    RevisionMetadata result = rh.getMetadata(Revision.create(1, "mockrepo"));
    assertEquals("1", result.id());
    assertEquals("foo@google.com", result.author());
    assertThat(result.date()).isEquivalentAccordingToCompareTo(DATE);
    assertEquals("description\n", result.description());
    assertThat(result.parents())
        .containsExactly(Revision.create(2, repositoryName), Revision.create(3, repositoryName))
        .inOrder();

    control.verify();
  }

  public void testParseMetadata_multiLine() throws CommandException {
    GitRevisionHistory rh =
        new GitRevisionHistory(Suppliers.ofInstance(mockClonedRepo(repositoryName)));

    control.replay();
    RevisionMetadata rm =
        rh.parseMetadata(
            METADATA_JOINER.join(
                "1",
                "foo@google.com",
                GIT_COMMIT_DATE,
                "2 3",
                "desc with \n\nmultiple lines\n"));
    control.verify();

    assertEquals("1", rm.id());
    assertEquals("foo@google.com", rm.author());
    assertThat(rm.date()).isEquivalentAccordingToCompareTo(DATE);
    assertEquals("desc with \n\nmultiple lines\n", rm.description());
    assertThat(rm.parents())
        .containsExactly(Revision.create(2, repositoryName), Revision.create(3, repositoryName))
        .inOrder();
  }

  /**
   * Mocks most of gh.findHeadRevisions(). Used by both of the next tests.
   *
   * @param mockRepo the mock repository to use
   */
  private void mockFindHeadRevisions(GitClonedRepository mockRepo) throws CommandException {
    expectLogCommand(mockRepo, LOG_FORMAT_COMMIT_ID, "HEAD").andReturn("head");
  }

  public void testFindNewRevisions_all() throws Exception {
    GitClonedRepository mockRepo = mockClonedRepo(repositoryName);
    mockFindHeadRevisions(mockRepo);
    DummyDb db = new DummyDb(false, null);

    // Breadth-first search order.
    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "head")
        .andReturn(
            METADATA_JOINER.join(
                "head", "uid@google.com", GIT_COMMIT_DATE, "parent1 parent2", "description"));

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "parent1")
        .andReturn(
            METADATA_JOINER.join("parent1", "uid@google.com", GIT_COMMIT_DATE, "", "description"));

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "parent2")
        .andReturn(
            METADATA_JOINER.join("parent2", "uid@google.com", GIT_COMMIT_DATE, "", "description"));

    control.replay();

    GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
    List<Revision> newRevisions =
        rh.findRevisions(null, new RepositoryEquivalenceMatcher("mockRepo", db), BRANCHED)
            .getRevisionsSinceEquivalence()
            .getBreadthFirstHistory();

    assertThat(newRevisions).hasSize(3);
    assertEquals(repositoryName, newRevisions.get(0).repositoryName());
    assertEquals("head", newRevisions.get(0).revId());
    assertEquals(repositoryName, newRevisions.get(1).repositoryName());
    assertEquals("parent1", newRevisions.get(1).revId());
    assertEquals(repositoryName, newRevisions.get(2).repositoryName());
    assertEquals("parent2", newRevisions.get(2).revId());

    control.verify();
  }

  public void testFindNewRevisions_pruned() throws Exception {
    GitClonedRepository mockRepo = mockClonedRepo(repositoryName);
    mockFindHeadRevisions(mockRepo);

    // Create a fake db that has equivalences for parent1, so that
    // parent1 isn't included in the output.
    DummyDb db =
        new DummyDb(true, null) {
          @Override
          public Set<Revision> findEquivalences(Revision revision, String otherRepository) {
            if (revision.revId().equals("parent1")) {
              return super.findEquivalences(revision, otherRepository);
            } else {
              return ImmutableSet.of();
            }
          }
        };

    // Breadth-first search order.
    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "head")
        .andReturn(
            METADATA_JOINER.join(
                "head", "uid@google.com", GIT_COMMIT_DATE, "parent1 parent2", "description"));

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "parent2")
        .andReturn(
            METADATA_JOINER.join("parent2", "uid@google.com", GIT_COMMIT_DATE, "", "description"));

    control.replay();

    GitRevisionHistory rh = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));
    List<Revision> newRevisions =
        rh.findRevisions(null, new RepositoryEquivalenceMatcher("mockRepo", db), BRANCHED)
            .getRevisionsSinceEquivalence()
            .getBreadthFirstHistory();

    // parent1 should not be traversed.
    assertThat(newRevisions).hasSize(2);
    assertEquals("head", newRevisions.get(0).revId());
    assertEquals("parent2", newRevisions.get(1).revId());

    control.verify();
  }

  /*
   * A database that holds the following equivalences:
   * repo1{1002} == repo2{2}
   */
  private final String testDb1 =
      "{\"equivalences\":["
          + "{\"rev1\": {\"revId\":\"1002\",\"repositoryName\":\"repo1\"},"
          + " \"rev2\": {\"revId\":\"2\",\"repositoryName\":\"repo2\"}}]}";

  /**
   * A test for finding the last equivalence for the following history starting
   * with repo2{4}:<pre>
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
   * </pre>
   *
   * @throws Exception
   */
  public void testFindLastEquivalence() throws Exception {
    GitClonedRepository mockRepo = mockClonedRepo("repo2");

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "4")
        .andReturn(METADATA_JOINER.join("4", "author", GIT_COMMIT_DATE, "3a 3b", "description"));

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "3a")
        .andReturn(METADATA_JOINER.join("3a", "author", GIT_COMMIT_DATE, "2", "description"));

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "3b")
        .andReturn(METADATA_JOINER.join("3b", "author", GIT_COMMIT_DATE, "2", "description"));

    control.replay();

    FileDb database =
        new FileDb(null, GsonModule.provideGson().fromJson(testDb1, DbStorage.class), null);

    GitRevisionHistory history = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));

    Result result =
        history.findRevisions(
            Revision.create(4, "repo2"),
            new RepositoryEquivalenceMatcher("repo1", database),
            SearchType.BRANCHED);

    RepositoryEquivalence expectedEq =
        RepositoryEquivalence.create(
            Revision.create(1002, "repo1"), Revision.create(2, "repo2"));

    assertThat(result.getEquivalences()).hasSize(1);
    assertEquals(expectedEq, result.getEquivalences().get(0));

    control.verify();
  }

  /*
   * A database that holds the following equivalences:
   * repo1{1005} == repo2{5}
   */
  private final String testDb2 =
      "{\"equivalences\":["
          + "{\"rev1\": {\"revId\":\"1005\",\"repositoryName\":\"repo1\"},"
          + " \"rev2\": {\"revId\":\"5\",\"repositoryName\":\"repo2\"}}]}";

  /**
   * A test for finding the last equivalence for the following history starting
   * with repo2{4}:<pre>
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
   * </pre>
   *
   * @throws Exception
   */
  public void testFindLastEquivalenceNull() throws Exception {
    GitClonedRepository mockRepo = mockClonedRepo("repo2");

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "4")
        .andReturn(METADATA_JOINER.join("4", "author", GIT_COMMIT_DATE, "3a 3b", "description"));

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "3a")
        .andReturn(METADATA_JOINER.join("3a", "author", GIT_COMMIT_DATE, "2", "description"));

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "3b")
        .andReturn(METADATA_JOINER.join("3b", "author", GIT_COMMIT_DATE, "2", "description"));

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "2")
        .andReturn(METADATA_JOINER.join("2", "author", GIT_COMMIT_DATE, "", "description"));

    control.replay();

    FileDb database =
        new FileDb(null, GsonModule.provideGson().fromJson(testDb2, DbStorage.class), null);

    GitRevisionHistory history = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));

    Result result =
        history.findRevisions(
            Revision.create(4, "repo2"),
            new RepositoryEquivalenceMatcher("repo1", database),
            SearchType.BRANCHED);

    assertThat(result.getEquivalences()).isEmpty();

    control.verify();
  }

  /**
   * A test for finding the last equivalence for the following history starting
   * with repo2{4} and <em>only searching linear history</em> instead of following multi-parent
   * commits:<pre>
   *
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
   * </pre>
   *
   * @throws Exception
   */
  public void testFindLastEquivalence_linearSearch() throws Exception {
    GitClonedRepository mockRepo = mockClonedRepo("repo2");

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "4")
        .andReturn(METADATA_JOINER.join("4", "author", GIT_COMMIT_DATE, "3a 3b", "description"));

    expectLogCommandIgnoringMissing(mockRepo, LOG_FORMAT_ALL_METADATA, "3a")
        .andReturn(METADATA_JOINER.join("3a", "author", GIT_COMMIT_DATE, "2", "description"));

    // Note revision 3b is <em>not</em> expected here for a linear history search.

    control.replay();

    FileDb database =
        new FileDb(null, GsonModule.provideGson().fromJson(testDb1, DbStorage.class), null);

    GitRevisionHistory history = new GitRevisionHistory(Suppliers.ofInstance(mockRepo));

    Result result =
        history.findRevisions(
            Revision.create(4, "repo2"),
            new RepositoryEquivalenceMatcher("repo1", database),
            SearchType.LINEAR);

    RepositoryEquivalence expectedEq =
        RepositoryEquivalence.create(
            Revision.create(1002, "repo1"), Revision.create(2, "repo2"));

    assertThat(result.getRevisionsSinceEquivalence().getBreadthFirstHistory())
        .containsExactly(Revision.create(4, "repo2"), Revision.create("3a", "repo2"))
        .inOrder();
    assertThat(result.getEquivalences()).containsExactly(expectedEq);

    control.verify();
  }
}
