// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Moe;
import com.google.devtools.moe.client.NullFileSystemModule;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.database.EquivalenceMatcher.EquivalenceMatchResult;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.DummyDb;
import com.google.devtools.moe.client.testing.MoeAsserts;
import com.google.devtools.moe.client.testing.TestingModule;

import dagger.Provides;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author dbentley@google.com
 *
 */
public class SvnRevisionHistoryTest extends TestCase {
  // Svn actually follows the spec!
  private static final String SVN_COMMIT_DATE = "2012-07-09T13:00:00.000000Z";
  private static final DateTime DATE =
      // 2012/7/9, 6am
      new DateTime(2012, 7, 9, 6, 0, DateTimeZone.forOffsetHours(-7));

  private final IMocksControl control = EasyMock.createControl();
  private final CommandRunner cmd = control.createMock(CommandRunner.class);

  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {TestingModule.class, NullFileSystemModule.class, Module.class})
  @Singleton
  interface Component extends Moe.Component {
    @Override Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @dagger.Module
  class Module {
    @Provides public CommandRunner commandRunner() {
      return cmd;
    }
  }

  @Override protected void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = DaggerSvnRevisionHistoryTest_Component.builder().module(new Module())
        .build().context();
  }

  public void testParseRevisions() {
    List<Revision> rs = SvnRevisionHistory.parseRevisions(
        "<log><logentry revision=\"1\"/></log>",
        "name");
    assertEquals(1, rs.size());
    assertEquals("1", rs.get(0).revId);
  }

  public void testGetHighestRevision() {
    try {
      expect(cmd.runCommand(
          "svn",
          ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "1", "-r", "HEAD:1",
                           "http://foo/svn/trunk/"),
          "")).andReturn("<log><logentry revision=\"3\" /></log>");
      expect(cmd.runCommand(
          "svn",
          ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "1", "-r", "2:1",
                           "http://foo/svn/trunk/"),
          "")).andReturn("<log><logentry revision=\"2\" /></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }
    control.replay();
    SvnRevisionHistory history = new SvnRevisionHistory("internal_svn",
        "http://foo/svn/trunk/");
    Revision result = history.findHighestRevision("");
    assertEquals(result.revId, "3");

    result = history.findHighestRevision("2");
    assertEquals(result.revId, "2");
    control.verify();
  }

  public void testParseMetadata() {
    SvnRevisionHistory history = new SvnRevisionHistory("internal_svn",
        "http://foo/svn/trunk/");
    List<RevisionMetadata> rs = history.parseMetadata(
        "<log><logentry revision=\"2\"><author>uid@google.com</author>" +
        "<date>" + SVN_COMMIT_DATE + "</date><msg>description</msg></logentry>" +
        "<logentry revision = \"1\"><author>user@google.com</author>" +
        "<date>" + SVN_COMMIT_DATE + "</date><msg>message</msg></logentry></log>");
    assertEquals(2, rs.size());
    assertEquals("2", rs.get(0).id);
    assertEquals("uid@google.com", rs.get(0).author);
    MoeAsserts.assertSameDate(DATE, rs.get(0).date);
    assertEquals("description", rs.get(0).description);
    assertEquals(ImmutableList.of(new Revision("1", "internal_svn")), rs.get(0).parents);
    assertEquals("1", rs.get(1).id);
    assertEquals("user@google.com", rs.get(1).author);
    MoeAsserts.assertSameDate(DATE, rs.get(1).date);
    assertEquals("message", rs.get(1).description);
    assertEquals(ImmutableList.of(), rs.get(1).parents);
  }

  public void testGetMetadata() {
    try {
      expect(cmd.runCommand(
          "svn",
          ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "2", "-r", "3:1",
                           "http://foo/svn/trunk/"),
          "")).andReturn("<log><logentry revision=\"3\">" +
                             "<author>uid@google.com</author>" +
                             "<date>" + SVN_COMMIT_DATE + "</date>" +
                             "<msg>message</msg></logentry>" +
                             "<logentry revision =\"2\">" +
                             "<author>user@google.com</author>" +
                             "<date>" + SVN_COMMIT_DATE + "</date>" +
                             "<msg>description</msg></logentry></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }
    control.replay();
    SvnRevisionHistory history = new SvnRevisionHistory("internal_svn",
        "http://foo/svn/trunk/");
    RevisionMetadata result = history.getMetadata(new Revision("3", "internal_svn"));
    assertEquals("3", result.id);
    assertEquals("uid@google.com", result.author);
    MoeAsserts.assertSameDate(DATE, result.date);
    assertEquals("message", result.description);
    assertEquals(ImmutableList.of(new Revision("2", "internal_svn")), result.parents);
    control.verify();
  }

  /**
   * Tests parsing of this SVN log entry:
   *
   * {@code
   * <logentry>
   *   <author>user</author>
   *   <text/>
   *   <date>yyyy-mm-dd</date>
   *   <msg>description</msg>
   * </logentry>
   * }
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

    SvnRevisionHistory history = new SvnRevisionHistory("internal_svn", "http://foo/svn/trunk/");

    RevisionMetadata result = history.parseMetadataNodeList(
        "7",
        doc.getElementsByTagName("logentry").item(0).getChildNodes(),
        ImmutableList.of(new Revision("6", "internal")));

    RevisionMetadata expected = new RevisionMetadata("7", "user",
        DATE, "description",
        ImmutableList.of(new Revision("6", "internal")));

    assertEquals(expected, result);
  }

  public void testFindNewRevisions() {
    DummyDb db = new DummyDb(false);

    // Mock call for findHighestRevision
    try {
      expect(cmd.runCommand(
          "svn",
          ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "1", "-r", "HEAD:1",
                           "http://foo/svn/trunk/"),
          "")).andReturn("<log><logentry revision=\"3\">" +
                             "<author>uid@google.com</author>" +
                             "<date>" + SVN_COMMIT_DATE + "</date>" +
                             "<msg>description</msg></logentry></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    // revision 3 metadata
    try {
      expect(cmd.runCommand(
          "svn",
          ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "2", "-r", "3:1",
                           "http://foo/svn/trunk/"),
          "")).andReturn("<log><logentry revision=\"3\">" +
                             "<author>uid@google.com</author>" +
                             "<date>" + SVN_COMMIT_DATE + "</date>" +
                             "<msg>message</msg></logentry>" +
                             "<logentry revision =\"2\">" +
                             "<author>user@google.com</author>" +
                             "<date>" + SVN_COMMIT_DATE + "</date>" +
                             "<msg>description</msg></logentry></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }


    // revision 2 metadata
    try {
      expect(cmd.runCommand(
          "svn",
          ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "2", "-r", "2:1",
                           "http://foo/svn/trunk/"),
          "")).andReturn("<log><logentry revision=\"2\">" +
                             "<author>uid@google.com</author>" +
                             "<date>" + SVN_COMMIT_DATE + "</date>" +
                             "<msg>description</msg></logentry></log>");
    } catch (CommandException e) {
      throw new RuntimeException(e);
    }

    control.replay();
    SvnRevisionHistory history = new SvnRevisionHistory("internal_svn",
        "http://foo/svn/trunk/");
    List<Revision> newRevisions =
        history.findRevisions(null, new EquivalenceMatcher("public", db), SearchType.LINEAR)
        .getRevisionsSinceEquivalence().getBreadthFirstHistory();
    assertEquals(2, newRevisions.size());
    assertEquals("internal_svn", newRevisions.get(0).repositoryName);
    assertEquals("3", newRevisions.get(0).revId);
    assertEquals("internal_svn", newRevisions.get(1).repositoryName);
    assertEquals("2", newRevisions.get(1).revId);
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
   *
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
   *
   * @throws Exception
   */
  public void testFindLastEquivalence() throws Exception {
    expect(cmd.runCommand("svn", ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "2",
        "-r", "4:1", "http://foo/svn/trunk/"), ""))
        .andReturn("<log><logentry revision=\"4\">" +
                   "<author>uid@google.com</author>" +
                   "<date>" + SVN_COMMIT_DATE + "</date>" +
                   "<msg>message</msg></logentry>" +
                   "<logentry revision =\"3\">" +
                   "<author>user@google.com</author>" +
                   "<date>" + SVN_COMMIT_DATE + "</date>" +
                   "<msg>description</msg></logentry></log>");

    expect(cmd.runCommand("svn", ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "2",
        "-r", "3:1", "http://foo/svn/trunk/"), ""))
        .andReturn("<log><logentry revision=\"3\">" +
                   "<author>uid@google.com</author>" +
                   "<date>" + SVN_COMMIT_DATE + "</date>" +
                   "<msg>message</msg></logentry>" +
                   "<logentry revision =\"2\">" +
                   "<author>user@google.com</author>" +
                   "<date>" + SVN_COMMIT_DATE + "</date>" +
                   "<msg>description</msg></logentry></log>");

    control.replay();

    FileDb database = FileDb.makeDbFromDbText(testDb1);
    SvnRevisionHistory history = new SvnRevisionHistory("repo2", "http://foo/svn/trunk/");

    EquivalenceMatchResult result = history.findRevisions(
        new Revision("4", "repo2"), new EquivalenceMatcher("repo1", database), SearchType.LINEAR);

    control.verify();

    Equivalence expectedEq = new Equivalence(new Revision("1002", "repo1"),
                                             new Revision("2", "repo2"));

    assertEquals(1, result.getEquivalences().size());
    assertEquals(expectedEq, result.getEquivalences().get(0));
  }

  /*
   * A database that holds the following equivalences:
   * repo1{1003} == repo2{3}
   */
  private final String testDb2 = "{\"equivalences\":["
      + "{\"rev1\": {\"revId\":\"1003\",\"repositoryName\":\"repo1\"},"
      + " \"rev2\": {\"revId\":\"3\",\"repositoryName\":\"repo2\"}}]}";

  /**
   * A test for finding the last equivalence for the following history starting
   * with repo2{2}:
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
   *
   * @throws Exception
   */
  public void testFindLastEquivalenceNull() throws Exception {
    expect(cmd.runCommand("svn", ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "2",
        "-r", "2:1", "http://foo/svn/trunk/"), ""))
        .andReturn("<log><logentry revision=\"2\">" +
                   "<author>uid@google.com</author>" +
                   "<date>" + SVN_COMMIT_DATE + "</date>" +
                   "<msg>message</msg></logentry>" +
                   "<logentry revision =\"1\">" +
                   "<author>user@google.com</author>" +
                   "<date>" + SVN_COMMIT_DATE + "</date>" +
                   "<msg>description</msg></logentry></log>");

    expect(cmd.runCommand("svn", ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "2",
        "-r", "1:1", "http://foo/svn/trunk/"), ""))
        .andReturn("<log><logentry revision=\"1\">" +
                   "<author>uid@google.com</author>" +
                   "<date>" + SVN_COMMIT_DATE + "</date>" +
                   "<msg>message</msg></logentry></log>");

    control.replay();

    FileDb database = FileDb.makeDbFromDbText(testDb2);
    SvnRevisionHistory history = new SvnRevisionHistory("repo2", "http://foo/svn/trunk/");

    EquivalenceMatchResult result = history.findRevisions(
        new Revision("2", "repo2"), new EquivalenceMatcher("repo1", database), SearchType.LINEAR);

    control.verify();

    assertEquals(0, result.getEquivalences().size());
  }
}
