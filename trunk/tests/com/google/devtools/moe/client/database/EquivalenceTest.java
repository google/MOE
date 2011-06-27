// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.devtools.moe.client.repositories.Revision;

import junit.framework.TestCase;

/**
 */
public class EquivalenceTest extends TestCase {

  public void testHasRevision() throws Exception {
    Equivalence e = new Equivalence(new Revision("r1", "name1"), new Revision("r2", "name2"));
    assertTrue(e.hasRevision(new Revision("r2", "name2")));
    assertFalse(e.hasRevision(new Revision("r1", "name3")));
  }

  public void testGetOtherRevision() throws Exception {
    Revision r1 = new Revision("r1", "name1");
    Revision r2 = new Revision("r2", "name2");
    Equivalence e = new Equivalence(r1, r2);
    assertEquals(e.getOtherRevision(r1), r2);
    assertEquals(e.getOtherRevision(r2), r1);
    assertNull(e.getOtherRevision(new Revision("r1", "name3")));
  }

  public void testEquals() throws Exception {
    Revision r1 = new Revision("r1", "name1");
    Revision r2 = new Revision("r2", "name2");
    Equivalence e1 = new Equivalence(r1, r2);
    Equivalence e2 = new Equivalence(r2, r1);
    assertTrue(e1.equals(e2));
    assertTrue(e2.equals(e1));
  }
}
