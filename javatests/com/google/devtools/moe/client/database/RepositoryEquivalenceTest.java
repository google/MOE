// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.devtools.moe.client.repositories.Revision;

import junit.framework.TestCase;

/**
 */
public class RepositoryEquivalenceTest extends TestCase {

  public void testHasRevision() throws Exception {
    RepositoryEquivalence e =
        RepositoryEquivalence.create(new Revision("r1", "name1"), new Revision("r2", "name2"));
    assertTrue(e.hasRevision(new Revision("r2", "name2")));
    assertFalse(e.hasRevision(new Revision("r1", "name3")));
  }

  public void testGetOtherRevision() throws Exception {
    Revision r1 = new Revision("r1", "name1");
    Revision r2 = new Revision("r2", "name2");
    RepositoryEquivalence e = RepositoryEquivalence.create(r1, r2);
    assertEquals(e.getOtherRevision(r1), r2);
    assertEquals(e.getOtherRevision(r2), r1);
    assertNull(e.getOtherRevision(new Revision("r1", "name3")));
  }

  public void testEquals() throws Exception {
    Revision r1 = new Revision("r1", "name1");
    Revision r2 = new Revision("r2", "name2");
    RepositoryEquivalence e1 = RepositoryEquivalence.create(r1, r2);
    RepositoryEquivalence e2 = RepositoryEquivalence.create(r2, r1);
    assertTrue(e1.equals(e2));
    assertTrue(e2.equals(e1));
  }

  public void testToString() throws Exception {
    Revision r1 = new Revision("r1", "name1");
    Revision r2 = new Revision("r2", "name2");
    RepositoryEquivalence e1 = RepositoryEquivalence.create(r1, r2);
    assertEquals("name1{r1} == name2{r2}", e1.toString());
  }
}
