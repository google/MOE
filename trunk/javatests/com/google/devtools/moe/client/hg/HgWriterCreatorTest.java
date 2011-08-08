// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

/**
 * Unit tests for HgWriterCreator.
 *
 */
public class HgWriterCreatorTest extends TestCase {

  public void testCreate() throws Exception {
    AppContextForTesting.initForTest();
    IMocksControl control = EasyMock.createControl();

    HgClonedRepository mockTipClone = control.createMock(HgClonedRepository.class);
    HgRevisionHistory mockRevHistory = control.createMock(HgRevisionHistory.class);

    String revId = "mockChangesetId";
    String repoName = "mockHgRepo";
    String projectSpace = "public";

    expect(mockRevHistory.findHighestRevision(revId)).andReturn(new Revision(revId, repoName));
    mockTipClone.updateToRevId(revId);

    control.replay();

    // No asserts, just check expected completion/failure.

    HgWriterCreator wc = new HgWriterCreator(mockTipClone, mockRevHistory, projectSpace);
    HgWriter w = (HgWriter) wc.create(ImmutableMap.of("revision", revId));

    try {
      HgWriter w2 = (HgWriter) wc.create(ImmutableMap.of("iron vise", revId));
      fail("HgWriterCreator.create() didn't fail when given options without 'revision' key.");
    } catch (MoeProblem expected) {}

    control.verify();
  }
}
