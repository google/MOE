// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.tools;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.codebase.Codebase;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Tests CodebaseDifference by actually invoking diff.
 * Tests FileDifference in the process.
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CodebaseDifferenceTest extends TestCase {

  public void testSame() throws Exception {
    IMocksControl control = EasyMock.createControl();
    Codebase c1 = control.createMock(Codebase.class);
    Codebase c2 = control.createMock(Codebase.class);
    File f1 = new File("/1/foo");
    File f2 = new File("/2/foo");
    FileDifference.FileDiffer differ = control.createMock(FileDifference.FileDiffer.class);

    expect(c1.getRelativeFilenames()).andReturn(ImmutableSet.of("foo"));
    expect(c2.getRelativeFilenames()).andReturn(ImmutableSet.of("foo"));
    expect(c1.getFile("foo")).andReturn(f1);
    expect(c2.getFile("foo")).andReturn(f2);
    expect(differ.diffFiles("foo", f1, f2)).andReturn(null);

    control.replay();
    CodebaseDifference d = CodebaseDifference.diffCodebases(c1, c2, differ);
    control.verify();

    assertEquals(false, d.areDifferent());
  }

  public void testDifferent() throws Exception {
    IMocksControl control = EasyMock.createControl();
    Codebase c1 = control.createMock(Codebase.class);
    Codebase c2 = control.createMock(Codebase.class);
    File f1 = new File("/1/foo");
    File f2 = new File("/2/foo");
    FileDifference.FileDiffer differ = control.createMock(FileDifference.FileDiffer.class);

    expect(c1.getRelativeFilenames()).andReturn(ImmutableSet.of("foo"));
    expect(c2.getRelativeFilenames()).andReturn(ImmutableSet.of("foo"));
    expect(c1.getFile("foo")).andReturn(f1);
    expect(c2.getFile("foo")).andReturn(f2);
    expect(differ.diffFiles("foo", f1, f2)).andReturn(null);

    control.replay();
    CodebaseDifference d = CodebaseDifference.diffCodebases(c1, c2, differ);
    control.verify();

    assertEquals(false, d.areDifferent());
  }

}
