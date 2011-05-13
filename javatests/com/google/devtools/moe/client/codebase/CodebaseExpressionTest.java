// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CodebaseExpressionTest extends TestCase {

  public void testParse() throws Exception {
    CodebaseExpression e = CodebaseExpression.parse("internal(revision=45)");
    assertEquals("internal", e.creator.identifier);
    assertEquals(ImmutableMap.of("revision", "45"), e.creator.options);
  }

  private void assertRoundTrip(String expected, String expression) throws Exception {
    CodebaseExpression e = CodebaseExpression.parse(expression);
    assertEquals(expected, e.toString());
  }

  public void testToString() throws Exception {
    assertRoundTrip("internal(revision=45)", "internal(revision=45)");
    assertRoundTrip("internal", "internal()");
    assertRoundTrip("internal|foo", "internal|foo");
    assertRoundTrip("internal|foo|bar", "internal|foo|bar");
    assertRoundTrip("internal|foo(a=b,c=d)|bar(e=f,g=h)",
                    "internal|foo (a = b, c=d)|bar(e=f  ,  g   =h)");
    assertRoundTrip("internal>public", "internal>public");
  }
}
