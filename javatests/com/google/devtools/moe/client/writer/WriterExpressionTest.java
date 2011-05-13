// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.writer;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class WriterExpressionTest extends TestCase {

  public void testParse() throws Exception {
    WriterExpression e = WriterExpression.parse("internal(revision=45)");
    assertEquals("internal", e.creator.identifier);
    assertEquals(ImmutableMap.of("revision", "45"), e.creator.options);
  }
}
