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

package com.google.devtools.moe.client.svn;

import static com.google.common.truth.Truth.assertThat;
import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.database.DbStorage;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher.Result;
import com.google.devtools.moe.client.GsonModule;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.DummyDb;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SvnRevisionHistoryTest extends TestCase {
  // Svn actually follows the spec!
  private static final String SVN_COMMIT_DATE = "2012-07-09T13:00:00.000000Z";
  private static final DateTime DATE =
      // 2012/7/9, 6am
      new DateTime(2012, 7, 9, 6, 0, DateTimeZone.forOffsetHours(-7));

  private final IMocksControl control = EasyMock.createControl();
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final SvnUtil util = new SvnUtil(cmd);

  public void testParseRevisions() {
    List<Revision> rs =
        SvnRevisionHistory.parseRevisions("<log><logentry revision=\"1\"/></log>", "name");
    assertEquals(1, rs.size());
    assertEquals("1", rs.get(0).revId());
  }

  public void testGetHighestRevision() {
    try {
      expect(
              cmd.runCommand(
                  "",
                  "svn",
                  ImmutableList.of(
                      "--no-auth-cache",
                      "log",
                      "--xml",
                      "-l",
                      "1",
                      "-r",
                      "HEAD:1",
                      "http://foo/svn/trunk/")))
          .andReturn("<log><logentry revision=\"3\" /></log>");
      expect(
              cmd.runCommand(
                  "",
                  "svn",
                  ImmutableList.of(
                      "--no-auth-cache",
                      "log",
                      "--xml",
                      "-l",
                      "1",
                      "-r",
                      "2:1",
                      "http://foo/svn/trunk/")))
          .andReturn("<log><logentry revision=\"2\" /></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }
    control.replay();
    SvnRevisionHistory history =
        new SvnRevisionHistory("internal_svn", "http://foo/svn/trunk/", util);
    Revision result = history.findHighestRevision("");
    assertEquals("3", result.revId());

    result = history.findHighestRevision("2");
    assertEquals("2", result.revId());
    control.verify();
  }

  public void testParseMetadata() {
    SvnRevisionHistory history =
        new SvnRevisionHistory("internal_svn", "http://foo/svn/trunk/", null);
    List<RevisionMetadata> rs =
        history.parseMetadata(
            "<log><logentry revision=\"2\"><author>uid@google.com</author>"
                + "<date>"
                + SVN_COMMIT_DATE
                + "</date><msg>description</msg></logentry>"
                + "<logentry revision = \"1\"><author>user@google.com</author>"
                + "<date>"
                + SVN_COMMIT_DATE
                + "</date><msg>message</msg></logentry></log>");
    assertEquals(2, rs.size());
    assertEquals("2", rs.get(0).id());
    assertEquals("uid@google.com", rs.get(0).author());
    assertThat(rs.get(0).date()).isEquivalentAccordingToCompareTo(DATE);
    assertEquals("description", rs.get(0).description());
    assertEquals(ImmutableList.of(Revision.create(1, "internal_svn")), rs.get(0).parents());
    assertEquals("1", rs.get(1).id());
    assertEquals("user@google.com", rs.get(1).author());
    assertThat(rs.get(1).date()).isEquivalentAccordingToCompareTo(DATE);
    assertEquals("message", rs.get(1).description());
    assertEquals(ImmutableList.of(), rs.get(1).parents());
  }

  public void testGetMetadata() {
    try {
      expect(
              cmd.runCommand(
                  "",
                  "svn",
                  ImmutableList.of(
                      "--no-auth-cache",
                      "log",
                      "--xml",
                      "-l",
                      "2",
                      "-r",
                      "3:1",
                      "http://foo/svn/trunk/")))
          .andReturn(
              "<log><logentry revision=\"3\">"
                  + "<author>uid@google.com</author>"
                  + "<date>"
                  + SVN_COMMIT_DATE
                  + "</date>"
                  + "<msg>message</msg></logentry>"
                  + "<logentry revision =\"2\">"
                  + "<author>user@google.com</author>"
                  + "<date>"
                  + SVN_COMMIT_DATE
                  + "</date>"
                  + "<msg>description</msg></logentry></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }
    control.replay();
    SvnRevisionHistory history =
        new SvnRevisionHistory("internal_svn", "http://foo/svn/trunk/", util);
    RevisionMetadata result = history.getMetadata(Revision.create(3, "internal_svn"));
    assertEquals("3", result.id());
    assertEquals("uid@google.com", result.author());
    assertThat(result.date()).isEquivalentAccordingToCompareTo(DATE);
    assertEquals("message", result.description());
    assertEquals(ImmutableList.of(Revision.create(2, "internal_svn")), result.parents());
    control.verify();
  }

  /**
   * Tests parsing of this SVN log entry:
   *
   * <pre>{@code
   * <logentry>
   *   <author>user</author>
   *   <text/>
   *   <date>yyyy-mm-dd</date>
   *   <msg>description</msg>
   * </logentry>
   * }</pre>
   */
  public void testParseMetadataNodeList() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    Element logEntry = doc.createElement("logentry");
    doc.appendChild(logEntry);

    Element author = doc.createElement("author");
    author.appendChild(doc.createTextNode("user"));
    logEntry.appendChild(author);

    // The XML parser often throws in random child nodes like this.
    logEntry.appendChild(doc.createElement("text"));

    Element date = doc.createElement("date");
    date.appendChild(doc.createTextNode(SVN_COMMIT_DATE));
    logEntry.appendChild(date);

    Element msg = doc.createElement("msg");
    msg.appendChild(doc.createTextNode("description"));
    logEntry.appendChild(msg);

    SvnRevisionHistory history =
        new SvnRevisionHistory("internal_svn", "http://foo/svn/trunk/", null);
    RevisionMetadata result =
        history.parseMetadataNodeList(
            "7",
            doc.getElementsByTagName("logentry").item(0).getChildNodes(),
            ImmutableList.of(Revision.create(6, "internal")));

    RevisionMetadata expected =
        RevisionMetadata.builder()
            .id("7")
            .author("user")
            .date(DATE)
            .description("description")
            .withParents(Revision.create(6, "internal"))
            .build();

    assertThat(result).isEqualTo(expected);
  }

  public void testFindNewRevisions() {
    DummyDb db = new DummyDb(false, null);

    // Mock call for findHighestRevision
    try {
      expect(
              cmd.runCommand(
                  "",
                  "svn",
                  ImmutableList.of(
                      "--no-auth-cache",
                      "log",
                      "--xml",
                      "-l",
                      "1",
                      "-r",
                      "HEAD:1",
                      "http://foo/svn/trunk/")))
          .andReturn(
              "<log><logentry revision=\"3\">"
                  + "<author>uid@google.com</author>"
                  + "<date>"
                  + SVN_COMMIT_DATE
                  + "</date>"
                  + "<msg>description</msg></logentry></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    // revision 3 metadata
    try {
      expect(
              cmd.runCommand(
                  "",
                  "svn",
                  ImmutableList.of(
                      "--no-auth-cache",
                      "log",
                      "--xml",
                      "-l",
                      "2",
                      "-r",
                      "3:1",
                      "http://foo/svn/trunk/")))
          .andReturn(
              "<log><logentry revision=\"3\">"
                  + "<author>uid@google.com</author>"
                  + "<date>"
                  + SVN_COMMIT_DATE
                  + "</date>"
                  + "<msg>message</msg></logentry>"
                  + "<logentry revision =\"2\">"
                  + "<author>user@google.com</author>"
                  + "<date>"
                  + SVN_COMMIT_DATE
                  + "</date>"
                  + "<msg>description</msg></logentry></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    // revision 2 metadata
    try {
      expect(
              cmd.runCommand(
                  "",
                  "svn",
                  ImmutableList.of(
                      "--no-auth-cache",
                      "log",
                      "--xml",
                      "-l",
                      "2",
                      "-r",
                      "2:1",
                      "http://foo/svn/trunk/")))
          .andReturn(
              "<log><logentry revision=\"2\">"
                  + "<author>uid@google.com</author>"
                  + "<date>"
                  + SVN_COMMIT_DATE
                  + "</date>"
                  + "<msg>description</msg></logentry></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    control.replay();
    SvnRevisionHistory history =
        new SvnRevisionHistory("internal_svn", "http://foo/svn/trunk/", util);
    List<Revision> newRevisions =
        history
            .findRevisions(null, new RepositoryEquivalenceMatcher("public", db), SearchType.LINEAR)
            .getRevisionsSinceEquivalence()
            .getBreadthFirstHistory();
    assertEquals(2, newRevisions.size());
    assertEquals("internal_svn", newRevisions.get(0).repositoryName());
    assertEquals("3", newRevisions.get(0).revId());
    assertEquals("internal_svn", newRevisions.get(1).repositoryName());
    assertEquals("2", newRevisions.get(1).revId());
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
   *                                              _____
   *                                             |     |
   *                                             |  4  |
   *                                             |_____|
   *                                                |
   *                                                |
   *                                                |
   *                                              __|__
   *                                             |     |
   *                                             |  3  |
   *                                             |_____|
   *                                                |
   *                                                |
   *                                                |
   *                   ____                       __|__
   *                  |    |                     |     |
   *                  |1002|=====================|  2  |
   *                  |____|                     |_____|
   *
   *                   repo1                      repo2
   * </pre>
   *
   * @throws Exception
   */
  public void testFindLastEquivalence() throws Exception {
    expect(
            cmd.runCommand(
                "",
                "svn",
                ImmutableList.of(
                    "--no-auth-cache",
                    "log",
                    "--xml",
                    "-l",
                    "2",
                    "-r",
                    "4:1",
                    "http://foo/svn/trunk/")))
        .andReturn(
            "<log><logentry revision=\"4\">"
                + "<author>uid@google.com</author>"
                + "<date>"
                + SVN_COMMIT_DATE
                + "</date>"
                + "<msg>message</msg></logentry>"
                + "<logentry revision =\"3\">"
                + "<author>user@google.com</author>"
                + "<date>"
                + SVN_COMMIT_DATE
                + "</date>"
                + "<msg>description</msg></logentry></log>");

    expect(
            cmd.runCommand(
                "",
                "svn",
                ImmutableList.of(
                    "--no-auth-cache",
                    "log",
                    "--xml",
                    "-l",
                    "2",
                    "-r",
                    "3:1",
                    "http://foo/svn/trunk/")))
        .andReturn(
            "<log><logentry revision=\"3\">"
                + "<author>uid@google.com</author>"
                + "<date>"
                + SVN_COMMIT_DATE
                + "</date>"
                + "<msg>message</msg></logentry>"
                + "<logentry revision =\"2\">"
                + "<author>user@google.com</author>"
                + "<date>"
                + SVN_COMMIT_DATE
                + "</date>"
                + "<msg>description</msg></logentry></log>");

    control.replay();

    FileDb database =
        new FileDb(null, GsonModule.provideGson().fromJson(testDb1, DbStorage.class), null);
    SvnRevisionHistory history = new SvnRevisionHistory("repo2", "http://foo/svn/trunk/", util);

    Result result =
        history.findRevisions(
            Revision.create(4, "repo2"),
            new RepositoryEquivalenceMatcher("repo1", database),
            SearchType.LINEAR);

    control.verify();

    RepositoryEquivalence expectedEq =
        RepositoryEquivalence.create(
            Revision.create(1002, "repo1"), Revision.create(2, "repo2"));

    assertEquals(1, result.getEquivalences().size());
    assertEquals(expectedEq, result.getEquivalences().get(0));
  }

  /*
   * A database that holds the following equivalences:
   * repo1{1003} == repo2{3}
   */
  private final String testDb2 =
      "{\"equivalences\":["
          + "{\"rev1\": {\"revId\":\"1003\",\"repositoryName\":\"repo1\"},"
          + " \"rev2\": {\"revId\":\"3\",\"repositoryName\":\"repo2\"}}]}";

  /**
   * A test for finding the last equivalence for the following history starting
   * with repo2{2}:<pre>
   *                   ____                       _____
   *                  |    |                     |     |
   *                  |1003|=====================|  3  |
   *                  |____|                     |_____|
   *                                                |
   *                                                |
   *                                                |
   *                                              __|__
   *                                             |     |
   *                                             |  2  |
   *                                             |_____|
   *                                                |
   *                                                |
   *                                                |
   *                                              __|__
   *                                             |     |
   *                                             |  1  |
   *                                             |_____|
   *
   *                   repo1                      repo2
   * </pre>
   *
   * @throws Exception
   */
  public void testFindLastEquivalenceNull() throws Exception {
    expect(
            cmd.runCommand(
                "",
                "svn",
                ImmutableList.of(
                    "--no-auth-cache",
                    "log",
                    "--xml",
                    "-l",
                    "2",
                    "-r",
                    "2:1",
                    "http://foo/svn/trunk/")))
        .andReturn(
            "<log><logentry revision=\"2\">"
                + "<author>uid@google.com</author>"
                + "<date>"
                + SVN_COMMIT_DATE
                + "</date>"
                + "<msg>message</msg></logentry>"
                + "<logentry revision =\"1\">"
                + "<author>user@google.com</author>"
                + "<date>"
                + SVN_COMMIT_DATE
                + "</date>"
                + "<msg>description</msg></logentry></log>");

    expect(
            cmd.runCommand(
                "",
                "svn",
                ImmutableList.of(
                    "--no-auth-cache",
                    "log",
                    "--xml",
                    "-l",
                    "2",
                    "-r",
                    "1:1",
                    "http://foo/svn/trunk/")))
        .andReturn(
            "<log><logentry revision=\"1\">"
                + "<author>uid@google.com</author>"
                + "<date>"
                + SVN_COMMIT_DATE
                + "</date>"
                + "<msg>message</msg></logentry></log>");

    control.replay();

    FileDb database =
        new FileDb(null, GsonModule.provideGson().fromJson(testDb2, DbStorage.class), null);
    SvnRevisionHistory history = new SvnRevisionHistory("repo2", "http://foo/svn/trunk/", util);

    Result result =
        history.findRevisions(
            Revision.create(2, "repo2"),
            new RepositoryEquivalenceMatcher("repo1", database),
            SearchType.LINEAR);

    control.verify();

    assertEquals(0, result.getEquivalences().size());
  }
}
