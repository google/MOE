// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Unit tests for GitDraftRevision.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitDraftRevisionTest extends TestCase {

  public void testGetLocation() {
    final File mockRepoPath = new File("/mockrepo");

    IMocksControl control = EasyMock.createControl();
    GitClonedRepository mockRevClone = control.createMock(GitClonedRepository.class);
    expect(mockRevClone.getLocalTempDir()).andReturn(mockRepoPath);

    control.replay();

    GitDraftRevision dr = new GitDraftRevision(Suppliers.ofInstance(mockRevClone));
    assertEquals(mockRepoPath.getAbsolutePath(), dr.getLocation());

    control.verify();
  }
}
