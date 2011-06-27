// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseExpression;

import java.io.File;

/**
 * Utilities to write MOE tests more easily.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class TestUtils {

  public static Codebase makeCodebase(String name) throws Exception {
    return new Codebase(new File("/" + name), "public", CodebaseExpression.parse(name));
  }

  public static Codebase makeCodebase(File f, String expression) throws Exception {
    return new Codebase(f, "public", CodebaseExpression.parse(expression));
  }

  public static Codebase makeCodebase(File f, String projectSpace, String expression)
      throws Exception {
    return new Codebase(f, projectSpace, CodebaseExpression.parse(expression));
  }
}
