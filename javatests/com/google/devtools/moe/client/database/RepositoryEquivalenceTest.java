// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.devtools.moe.client.repositories.Revision;

import junit.framework.TestCase;

/**
 */
public class RepositoryEquivalenceTest extends TestCase {

  public void testHasRevision() throws Exception {
    RepositoryEquivalence e =
        RepositoryEquivalence.create(
            Revision.create(1, "name1"), Revision.create(2, "name2"));
    assertTrue(e.hasRevision(Revision.create(2, "name2")));
    assertFalse(e.hasRevision(Revision.create(1, "name3")));
  }

  public void testGetOtherRevision() throws Exception {
    Revision r1 = Revision.create(1, "name1");
    Revision r2 = Revision.create(2, "name2");
    RepositoryEquivalence e = RepositoryEquivalence.create(r1, r2);
    assertEquals(e.getOtherRevision(r1), r2);
    assertEquals(e.getOtherRevision(r2), r1);
    assertNull(e.getOtherRevision(Revision.create(1, "name3")));
  }
}
