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

import static com.google.common.truth.Truth.assertThat;
import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.NullFileSystemModule;
import com.google.devtools.moe.client.Ui.UiModule;
import com.google.devtools.moe.client.database.DbStorage;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher.Result;
import com.google.devtools.moe.client.gson.GsonModule;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.DummyDb;
import com.google.devtools.moe.client.testing.TestingModule;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.util.List;

import javax.inject.Singleton;

public class HgRevisionHistoryTest extends TestCase {
  private static final String HG_COMMIT_DATE = "2012-07-09 06:00 -0700";
  private static final DateTime DATE =
      // 2012/7/9, 6am
      new DateTime(2012, 7, 9, 6, 0, DateTimeZone.forOffsetHours(-7));

  private static final String MOCK_REPO_NAME = "mockrepo";
  private static final String CLONE_TEMP_DIR = "/tmp/hg_tipclone_mockrepo_12345";

  private final IMocksControl control = EasyMock.createControl();
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final RepositoryConfig config = control.createMock(RepositoryConfig.class);

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(
    modules = {TestingModule.class, NullFileSystemModule.class, Module.class, UiModule.class}
  )
  @Singleton
  interface Component {
    Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module
  class Module {
    @Provides
    public CommandRunner cmd() {
      return cmd;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE =
        DaggerHgRevisionHistoryTest_Component.builder().module(new Module()).build().context();
  }

  private HgClonedRepository mockClonedRepo(String repoName) {
    HgClonedRepository mockRepo = control.createMock(HgClonedRepository.class);
    expect(mockRepo.getRepositoryName()).andReturn(repoName).anyTimes();
    expect(mockRepo.getLocalTempDir()).andReturn(new File(CLONE_TEMP_DIR)).anyTimes();
    expect(mockRepo.getBranch()).andReturn("mybranch").anyTimes();
    expect(mockRepo.getConfig()).andReturn(config).anyTimes();
    return mockRepo;
  }

  public void testFindHighestRevision() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.<String>of(
                    "log", "--branch=mybranch", "--limit=1", "--template={node}"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("mockChangesetID");

    control.replay();

    HgRevisionHistory revHistory = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    Revision rev = revHistory.findHighestRevision(null);
    assertEquals(MOCK_REPO_NAME, rev.repositoryName());
    assertEquals("mockChangesetID", rev.revId());

    control.verify();
  }

  public void testFindHighestRevision_nonExistentChangesetThrows() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.<String>of(
                    "log",
                    "--branch=mybranch",
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
      revHistory.findHighestRevision("bogusChangeset");
      fail("'hg log' didn't fail on bogus changeset ID");
    } catch (MoeProblem expected) {
    }

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
                    "--template={node|escape} < {author|escape} < "
                        + "{date|isodate|escape} < {desc|escape} < "
                        + "{parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn(
            "2 < uid@google.com < " + HG_COMMIT_DATE + " < description < 1:parent1 2:parent2");

    control.replay();

    HgRevisionHistory revHistory = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    RevisionMetadata result = revHistory.getMetadata(Revision.create(2, "mockrepo"));
    assertEquals("2", result.id);
    assertEquals("uid@google.com", result.author);
    assertThat(result.date).isEquivalentAccordingToCompareTo(DATE);
    assertEquals("description", result.description);
    assertEquals(
        ImmutableList.of(
            Revision.create("parent1", MOCK_REPO_NAME), Revision.create("parent2", MOCK_REPO_NAME)),
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
                    "--template={node|escape} < {author|escape} < "
                        + "{date|isodate|escape} < {desc|escape} < "
                        + "{parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn(
            "2 < u&lt;id@google.com < " + HG_COMMIT_DATE + " < &gt;description&amp;amp < 1:parent");

    control.replay();

    HgRevisionHistory revHistory = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    RevisionMetadata result = revHistory.getMetadata(Revision.create(2, "mockrepo"));
    assertEquals("2", result.id);
    assertEquals("u<id@google.com", result.author);
    assertThat(result.date).isEquivalentAccordingToCompareTo(DATE);
    assertEquals(">description&amp", result.description);
    assertEquals(ImmutableList.of(Revision.create("parent", MOCK_REPO_NAME)), result.parents);

    control.verify();
  }

  public void testParseMetadata() throws Exception {
    HgRevisionHistory rh =
        new HgRevisionHistory(Suppliers.ofInstance(mockClonedRepo(MOCK_REPO_NAME)));

    control.replay();

    RevisionMetadata rm =
        rh.parseMetadata("1 < foo@google.com < " + HG_COMMIT_DATE + " < foo < 1:p1 -1:p2\n");
    assertEquals("1", rm.id);
    assertEquals("foo@google.com", rm.author);
    assertThat(rm.date).isEquivalentAccordingToCompareTo(DATE);
    assertEquals("foo", rm.description);
    assertEquals(ImmutableList.of(Revision.create("p1", MOCK_REPO_NAME)), rm.parents);

    control.verify();
  }

  public void testFindHeadRevisions() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.<String>of("heads", "mybranch", "--template={node} {branch}\n"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("mockChangesetID1 branch1\nmockChangesetID2 branch2\nmockChangesetID3 unused");

    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    ImmutableList<Revision> revs = ImmutableList.copyOf(rh.findHeadRevisions());
    assertEquals(MOCK_REPO_NAME, revs.get(0).repositoryName());
    assertEquals("mockChangesetID1", revs.get(0).revId());
    assertEquals(MOCK_REPO_NAME, revs.get(1).repositoryName());
    assertEquals("mockChangesetID2", revs.get(1).revId());

    control.verify();
  }

  public void testFindNewRevisions() throws Exception {
    HgClonedRepository mockRepo = mockClonedRepo(MOCK_REPO_NAME);
    DummyDb db = new DummyDb(false);

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.<String>of("heads", "mybranch", "--template={node} {branch}\n"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("mockChangesetID default\n");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.<String>of(
                    "log",
                    "--rev=mockChangesetID",
                    "--limit=1",
                    "--template={node|escape} < {author|escape} < "
                        + "{date|isodate|escape} < {desc|escape} < "
                        + "{parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn(
            "mockChangesetID < uid@google.com < " + HG_COMMIT_DATE + " < description < 1:parent");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.<String>of(
                    "log",
                    "--rev=parent",
                    "--limit=1",
                    "--template={node|escape} < {author|escape} < "
                        + "{date|isodate|escape} < {desc|escape} < "
                        + "{parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("parent < uid@google.com < " + HG_COMMIT_DATE + " < description < ");

    control.replay();

    HgRevisionHistory rh = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    List<Revision> newRevisions =
        rh
            .findRevisions(null, new RepositoryEquivalenceMatcher("public", db), SearchType.LINEAR)
            .getRevisionsSinceEquivalence()
            .getBreadthFirstHistory();
    assertEquals(2, newRevisions.size());
    assertEquals(MOCK_REPO_NAME, newRevisions.get(0).repositoryName());
    assertEquals("mockChangesetID", newRevisions.get(0).revId());
    assertEquals(MOCK_REPO_NAME, newRevisions.get(1).repositoryName());
    assertEquals("parent", newRevisions.get(1).revId());

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
    // Mock cloned repo
    HgClonedRepository mockRepo = mockClonedRepo("repo2");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.<String>of("heads", "mybranch", "--template={node} {branch}\n"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("4 default\n");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.of(
                    "log",
                    "--rev=4",
                    "--limit=1",
                    "--template={node|escape} < {author|escape} < {date|isodate|escape} < "
                        + "{desc|escape} < {parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("4 < author < " + HG_COMMIT_DATE + " < description < par1:3a par2:3b");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.of(
                    "log",
                    "--rev=3a",
                    "--limit=1",
                    "--template={node|escape} < {author|escape} < {date|isodate|escape} < "
                        + "{desc|escape} < {parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("3a < author < " + HG_COMMIT_DATE + " < description < par1:2 -1:0");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.of(
                    "log",
                    "--rev=3b",
                    "--limit=1",
                    "--template={node|escape} < {author|escape} < {date|isodate|escape} < "
                        + "{desc|escape} < {parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("3b < author < " + HG_COMMIT_DATE + " < description < par1:2 -1:0");

    control.replay();

    FileDb database = new FileDb(null, GsonModule.provideGson().fromJson(testDb1, DbStorage.class));

    HgRevisionHistory history = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));

    Result result =
        history.findRevisions(
            null, new RepositoryEquivalenceMatcher("repo1", database), SearchType.BRANCHED);

    RepositoryEquivalence expectedEq =
        RepositoryEquivalence.create(
            Revision.create(1002, "repo1"), Revision.create(2, "repo2"));
    assertEquals(ImmutableList.of(expectedEq), result.getEquivalences());

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
   * <pre>
   *
   * @throws Exception
   */
  public void testFindLastEquivalenceNull() throws Exception {
    // Mock cloned repo
    HgClonedRepository mockRepo = mockClonedRepo("repo2");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.of(
                    "log",
                    "--rev=4",
                    "--limit=1",
                    "--template={node|escape} < {author|escape} < {date|isodate|escape} < "
                        + "{desc|escape} < {parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("4 < author < " + HG_COMMIT_DATE + " < description < par1:3a par2:3b");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.of(
                    "log",
                    "--rev=3a",
                    "--limit=1",
                    "--template={node|escape} < {author|escape} < {date|isodate|escape} < "
                        + "{desc|escape} < {parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("3a < author < " + HG_COMMIT_DATE + " < description < par1:2 -1:0");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.of(
                    "log",
                    "--rev=3b",
                    "--limit=1",
                    "--template={node|escape} < {author|escape} < {date|isodate|escape} < "
                        + "{desc|escape} < {parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("3b < author < " + HG_COMMIT_DATE + " < description < par1:2 -1:0");

    expect(
            cmd.runCommand(
                "hg",
                ImmutableList.of(
                    "log",
                    "--rev=2",
                    "--limit=1",
                    "--template={node|escape} < {author|escape} < {date|isodate|escape} < "
                        + "{desc|escape} < {parents|stringify|escape}",
                    "--debug"),
                CLONE_TEMP_DIR /*workingDirectory*/))
        .andReturn("2 < author < " + HG_COMMIT_DATE + " < description < -1:0 -1:0");

    control.replay();

    FileDb database = new FileDb(null, GsonModule.provideGson().fromJson(testDb2, DbStorage.class));

    HgRevisionHistory history = new HgRevisionHistory(Suppliers.ofInstance(mockRepo));
    Result result =
        history.findRevisions(
            Revision.create(4, "repo2"),
            new RepositoryEquivalenceMatcher("repo1", database),
            SearchType.BRANCHED);

    assertEquals(0, result.getEquivalences().size());

    control.verify();
  }
}
