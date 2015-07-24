// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.common.base.Strings;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class InvalidProject extends Exception {
  public InvalidProject(String explanationTemplate, Object... args) {
    super(String.format(explanationTemplate, args));
  }

  // TODO(cgruber) figure out who uses these and delete them.
  public static void assertNotEmpty(String str, String message) throws InvalidProject {
    assertFalse(Strings.isNullOrEmpty(str), message);
  }

  public static void assertNotNull(Object obj, String message) throws InvalidProject {
    assertFalse(obj == null, message);
  }

  public static void assertTrue(boolean expr, String message) throws InvalidProject {
    if (!expr) {
      throw new InvalidProject(message);
    }
  }

  public static void assertFalse(boolean expr, String message) throws InvalidProject {
    if (expr) {
      throw new InvalidProject(message);
    }
  }
}
