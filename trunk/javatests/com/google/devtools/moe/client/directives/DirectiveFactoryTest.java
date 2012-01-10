// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DirectiveFactoryTest extends TestCase {

  public void testMakeDirective() throws Exception {
    Directive d = DirectiveFactory.makeDirective("highest_revision");
    assertTrue(d instanceof HighestRevisionDirective);
  }

  public void testBadInput() throws Exception {
    assertNull(DirectiveFactory.makeDirective("galsjdlkfj"));
  }
}
