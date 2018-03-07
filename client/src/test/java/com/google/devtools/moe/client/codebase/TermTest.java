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

package com.google.devtools.moe.client.codebase;

import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;

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
