// Copyright 2012 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import junit.framework.Assert;

import org.joda.time.DateTime;

/** Asserts used by MOE. */
public class MoeAsserts {
  private MoeAsserts() {} // All static

  public static void assertSameDate(DateTime expected, DateTime actual) {
    Assert.assertTrue(
        String.format("Expected %s, Actual %s", expected, actual),
        expected.isEqual(actual));
  }
}
