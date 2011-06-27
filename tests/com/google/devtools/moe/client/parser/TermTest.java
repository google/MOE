// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.parser;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class TermTest extends TestCase {

  public void testToString() throws Exception {
    Term t;

    t = new Term("internal", ImmutableMap.<String, String>of());
    assertEquals("internal", t.toString());

    t = new Term("internal", ImmutableMap.of("foo", "bar"));
    assertEquals("internal(foo=bar)", t.toString());

    t = new Term("internal", ImmutableMap.of("foo", "bar", "baz", "quux"));
    // We sort arguments.
    assertEquals("internal(baz=quux,foo=bar)", t.toString());

    t = new Term("inte rnal", ImmutableMap.of("foo", "bar", "baz", "qu ux"));
    assertEquals("\"inte rnal\"(baz=\"qu ux\",foo=bar)", t.toString());
  }
}
