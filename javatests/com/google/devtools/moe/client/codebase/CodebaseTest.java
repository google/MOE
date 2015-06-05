// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.MoeProblem;

import java.io.File;
import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CodebaseTest extends TestCase {

  public void testCheckProjectSpace() throws Exception {
    Codebase c =
        new Codebase(
            new File("/foo"),
            "internal",
            new RepositoryExpression(new Term("foo", ImmutableMap.<String, String>of())));
    c.checkProjectSpace("internal");
    try {
      c =
          new Codebase(
              new File("/foo"),
              "internal",
              new RepositoryExpression(new Term("foo", ImmutableMap.<String, String>of())));
      c.checkProjectSpace("public");
      fail();
    } catch (MoeProblem p) {
    }
  }
}
