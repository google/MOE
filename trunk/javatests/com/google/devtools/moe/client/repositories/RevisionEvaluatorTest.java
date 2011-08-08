// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.svn.SvnRevisionHistory;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.util.List;

/**
 *
 */
public class RevisionEvaluatorTest extends TestCase {

  private IMocksControl control;
  private SvnRevisionHistory svnRevHist;
  private Repository repo;
  private ImmutableMap<String, Repository> repos;
  private ProjectContext projCon;
  private Revision rev;

  @Override
  public void setUp() {
    control = EasyMock.createControl();
    svnRevHist = control.createMock(SvnRevisionHistory.class);
    repo = new Repository("svn", svnRevHist, null, null);
    repos = ImmutableMap.of("foo", repo);
    projCon = new ProjectContext(null, repos, null, null, null);
    rev = new Revision("45", "svn");
  }

  public void testEvaluate() throws Exception {
    expect(svnRevHist.findHighestRevision("45")).andReturn(rev);
    control.replay();
    List<Revision> revs = RevisionEvaluator.parseAndEvaluate("foo{45}", projCon);
    control.verify();
  }

}
