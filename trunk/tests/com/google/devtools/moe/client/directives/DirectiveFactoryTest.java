// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.directives;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DirectiveFactoryTest extends TestCase {

  public void testMakeDirective() throws Exception {
    Directive d = DirectiveFactory.makeDirective("hello");
    assertTrue(d instanceof HelloDirective);
  }

  public void testBadInput() throws Exception {
    assertNull(DirectiveFactory.makeDirective("galsjdlkfj"));
  }
}
