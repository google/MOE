// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

/**
 * Unit tests for GitWriterCreator.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitWriterCreatorTest extends TestCase {

  public void testCreate() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();

    GitClonedRepository mockHeadClone = control.createMock(GitClonedRepository.class);
    GitRevisionHistory mockRevHistory = control.createMock(GitRevisionHistory.class);

    String revId = "mockChangesetId";
    String repoName = "mockGitRepo";
    String projectSpace = "public";

    expect(mockRevHistory.findHighestRevision(revId)).andReturn(new Revision(revId, repoName));
    mockHeadClone.updateToRevId(revId);

    control.replay();

    // No asserts, just check expected completion/failure.

    GitWriterCreator wc = new GitWriterCreator(Suppliers.ofInstance(mockHeadClone), 
        mockRevHistory, projectSpace);
    GitWriter w = (GitWriter) wc.create(ImmutableMap.of("revision", revId));

    try {
      GitWriter w2 = (GitWriter) wc.create(ImmutableMap.of("iron vise", revId));
      fail("GitWriterCreator.create() didn't fail when given options without 'revision' key.");
    } catch (MoeProblem expected) {}

    control.verify();
  }
}
