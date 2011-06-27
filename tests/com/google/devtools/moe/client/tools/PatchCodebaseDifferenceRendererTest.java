// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.tools;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.testing.TestUtils;

import junit.framework.TestCase;

import java.io.File;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class PatchCodebaseDifferenceRendererTest extends TestCase {
  public void testRender() throws Exception {

    Codebase c1 = TestUtils.makeCodebase("c1");
    Codebase c2 = TestUtils.makeCodebase("c2");

    ImmutableSet.Builder<FileDifference> diffs = ImmutableSet.builder();

    diffs.add(new FileDifference(
        "foo", new File("/c1/foo"), new File("/c2/foo"),
        FileDifference.Comparison.SAME, FileDifference.Comparison.SAME, "> foo"));

    diffs.add(new FileDifference(
        "bar", new File("/c1/bar"), new File("/c2/bar"),
        FileDifference.Comparison.SAME, FileDifference.Comparison.ONLY2, null));

    diffs.add(new FileDifference(
        "baz", new File("/c1/baz"), new File("/c2/baz"),
        FileDifference.Comparison.SAME, FileDifference.Comparison.ONLY1, null));

    diffs.add(new FileDifference(
        "quux", new File("/c1/quux"), new File("/c2/quux"),
        FileDifference.Comparison.SAME, FileDifference.Comparison.ONLY2, "> quux"));

    diffs.add(new FileDifference(
        "fuzzy", new File("/c1/fuzzy"), new File("/c2/fuzzy"),
        FileDifference.Comparison.ONLY2, FileDifference.Comparison.SAME, "> fuzzy"));

    CodebaseDifference d = new CodebaseDifference(c1, c2, diffs.build());
    assertEquals("diff c1 c2\ndiff --moe c1/foo c2/foo\n<<< c1/foo\n>>> c2/foo\n" +
                 "> foo\ndiff --moe c1/bar c2/bar\n+mode:executable\n<<< c1/bar\n" +
                 ">>> c2/bar\ndiff --moe c1/baz c2/baz\n-mode:executable\n<<< c1/baz\n" +
                 ">>> c2/baz\ndiff --moe c1/quux c2/quux\n+mode:executable\n<<< c1/quux\n" +
                 ">>> c2/quux\n> quux\ndiff --moe c1/fuzzy c2/fuzzy\n<<< c1/fuzzy\n" +
                 ">>> c2/fuzzy\n> fuzzy\n",
                 (new PatchCodebaseDifferenceRenderer()).render(d));
  }
}
