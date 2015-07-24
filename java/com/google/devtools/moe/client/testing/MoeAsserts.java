// Copyright 2012 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import org.joda.time.DateTime;
import org.junit.Assert;

/** Asserts used by MOE. */
public class MoeAsserts {
  private MoeAsserts() {} // All static

  public static void assertSameDate(DateTime expected, DateTime actual) {
    Assert.assertTrue(
        String.format("Expected %s, Actual %s", expected, actual), expected.isEqual(actual));
  }

  public static void fail(String message, Object... args) {
    Assert.fail(String.format(message, args));
  }
}
