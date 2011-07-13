// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.repositories.RevisionExpression.RevisionExpressionError;

import junit.framework.TestCase;


/**
 *
 */
public class RevisionExpressionTest extends TestCase {

  /**
   * Helper method that asserts that expr is an invalid RevisionExpression.
   * RevisionExpressionError should be thrown when expr is parsed.
   */
  private void assertBadParse(String expr) throws Exception {
    try {
      RevisionExpression.parse(expr);
      fail("The RevisionExpression \"" + expr + "\" parsed correctly but"
          + "should have thrown an exception.");
    } catch (RevisionExpressionError e) {
      assertTrue(e.getMessage().startsWith("Revision Expression Error"));
    }
  }

  public void testParse() throws Exception {
    RevisionExpression r = RevisionExpression.parse("googlecode{3435,3436}");
    assertEquals("googlecode", r.repoId);
    assertEquals(ImmutableList.of("3435", "3436"), r.revIds);

    RevisionExpression r2 = RevisionExpression.parse("myrepo{rev45, 33 , foo , bar}");
    assertEquals("myrepo", r2.repoId);
    assertEquals(ImmutableList.of("rev45", "33", "foo", "bar"), r2.revIds);

    RevisionExpression r3 = RevisionExpression.parse("foo{45}");
    assertEquals("foo", r3.repoId);
    assertEquals(ImmutableList.of("45"), r3.revIds);

    RevisionExpression r4 = RevisionExpression.parse("myrepo");
    assertEquals("myrepo", r4.repoId);
    assertTrue(r4.revIds.isEmpty());

  }

  public void testBadParse() throws Exception {
    assertBadParse("googlecode{}");
    assertBadParse("googlecode{34343,}");
    assertBadParse("googlecode{34343,3433");
  }

  public void testToString() throws Exception {
    RevisionExpression r = RevisionExpression.parse("public{234,434}");
    assertEquals("public{234,434}", r.toString());

    assertEquals("internal", RevisionExpression.parse("internal").toString());
  }

  public void testIsValid() throws Exception {
    assertTrue(RevisionExpression.isValid("repo{rev3,43,rev22}"));
    assertFalse(RevisionExpression.isValid("repo{}"));
    assertTrue(RevisionExpression.isValid("googlecode"));
  }
}
