/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.tools;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import java.io.File;
import junit.framework.TestCase;

public class PatchCodebaseDifferenceRendererTest extends TestCase {

  private static Codebase makeCodebase(String name) throws Exception {
    return Codebase.create(new File("/" + name), "public", new RepositoryExpression(name));
  }

  public void testRender() throws Exception {

    Codebase c1 = makeCodebase("c1");
    Codebase c2 = makeCodebase("c2");

    ImmutableSet.Builder<FileDifference> diffs = ImmutableSet.builder();

    diffs.add(
        FileDifference.create(
            "foo",
            new File("/c1/foo"),
            new File("/c2/foo"),
            FileDifference.Comparison.SAME,
            FileDifference.Comparison.SAME,
            "> foo"));

    diffs.add(
        FileDifference.create(
            "bar",
            new File("/c1/bar"),
            new File("/c2/bar"),
            FileDifference.Comparison.SAME,
            FileDifference.Comparison.ONLY2,
            null));

    diffs.add(
        FileDifference.create(
            "baz",
            new File("/c1/baz"),
            new File("/c2/baz"),
            FileDifference.Comparison.SAME,
            FileDifference.Comparison.ONLY1,
            null));

    diffs.add(
        FileDifference.create(
            "quux",
            new File("/c1/quux"),
            new File("/c2/quux"),
            FileDifference.Comparison.SAME,
            FileDifference.Comparison.ONLY2,
            "> quux"));

    diffs.add(
        FileDifference.create(
            "fuzzy",
            new File("/c1/fuzzy"),
            new File("/c2/fuzzy"),
            FileDifference.Comparison.ONLY2,
            FileDifference.Comparison.SAME,
            "> fuzzy"));

    CodebaseDifference d = new CodebaseDifference(c1, c2, diffs.build());
    assertEquals(
        "diff c1 c2\ndiff --moe c1/foo c2/foo\n<<< c1/foo\n>>> c2/foo\n"
            + "> foo\ndiff --moe c1/bar c2/bar\n+mode:executable\n<<< c1/bar\n"
            + ">>> c2/bar\ndiff --moe c1/baz c2/baz\n-mode:executable\n<<< c1/baz\n"
            + ">>> c2/baz\ndiff --moe c1/quux c2/quux\n+mode:executable\n<<< c1/quux\n"
            + ">>> c2/quux\n> quux\ndiff --moe c1/fuzzy c2/fuzzy\n<<< c1/fuzzy\n"
            + ">>> c2/fuzzy\n> fuzzy\n",
        (new PatchCodebaseDifferenceRenderer()).render(d));
  }
}
