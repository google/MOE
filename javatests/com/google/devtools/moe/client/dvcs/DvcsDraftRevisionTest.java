// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs;

import static org.easymock.EasyMock.expect;

import com.google.devtools.moe.client.codebase.LocalClone;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 */
public class DvcsDraftRevisionTest extends TestCase {

  public void testGetLocation() {
    final File mockRepoPath = new File("/mockrepo");

    IMocksControl control = EasyMock.createControl();
    LocalClone mockRevClone = control.createMock(LocalClone.class);
    expect(mockRevClone.getLocalTempDir()).andReturn(mockRepoPath);

    control.replay();

    DvcsDraftRevision dr = new DvcsDraftRevision(mockRevClone);
    assertEquals(mockRepoPath.getAbsolutePath(), dr.getLocation());

    control.verify();
  }
}
