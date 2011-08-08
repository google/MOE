// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import static org.easymock.EasyMock.expect;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Unit tests for HgDraftRevision.
 *
 */
public class HgDraftRevisionTest extends TestCase {

  public void testGetLocation() {

    final File mockRepoPath = new File("/mockrepo");

    IMocksControl control = EasyMock.createControl();
    HgClonedRepository mockRevClone = control.createMock(HgClonedRepository.class);
    expect(mockRevClone.getLocalTempDir()).andReturn(mockRepoPath);

    control.replay();

    HgDraftRevision dr = new HgDraftRevision(mockRevClone);
    assertEquals(mockRepoPath.getAbsolutePath(), dr.getLocation());

    control.verify();
  }
}
