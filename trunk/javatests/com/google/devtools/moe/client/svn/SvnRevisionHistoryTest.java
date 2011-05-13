// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;
import junit.framework.TestCase;

import java.util.List;

/**
 * @author dbentley@google.com
 *
 */
public class SvnRevisionHistoryTest extends TestCase {

  public void testParseRevisions() {
    List<Revision> rs = SvnRevisionHistory.parseRevisions(
        "<log><logentry revision=\"1\"/></log>",
        "name");
    assertEquals(1, rs.size());
    assertEquals("1", rs.get(0).revId);
  }

  public void testGetHighestRevision() {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();
    CommandRunner cmd = control.createMock(CommandRunner.class);
    AppContext.RUN.cmd = cmd;

    try {
      expect(cmd.runCommand(
          "svn", 
          ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "1", "-r", "HEAD:1", 
                           "http://foo/svn/trunk/"), 
          "", "")).andReturn("<log><logentry revision=\"3\" /></log>");
      expect(cmd.runCommand(
          "svn",
          ImmutableList.of("--no-auth-cache", "log", "--xml", "-l", "1", "-r", "2:1", 
                           "http://foo/svn/trunk/"),
          "", "")).andReturn("<log><logentry revision=\"2\" /></log>");
    } catch (Exception e) {}
    control.replay();
    SvnRevisionHistory history = new SvnRevisionHistory("internal_svn", 
        "http://foo/svn/trunk/");
    Revision result = history.findHighestRevision("");
    assertEquals(result.revId, "3");

    result = history.findHighestRevision("2");
    assertEquals(result.revId, "2");
    control.verify();
}
  
}
