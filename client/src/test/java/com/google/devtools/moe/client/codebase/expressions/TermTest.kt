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

package com.google.devtools.moe.client.codebase.expressions

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TermTest {
  @Test fun testSimpleToString() {
    assertThat(Term("internal").toString()).isEqualTo("internal")
  }

  @Test fun testToStringWithOption() {
    val term = Term("internal").withOption("foo", "bar")
    assertThat(term.toString()).isEqualTo("internal(foo=bar)")
  }

  @Test fun testToStringWithSortedOptions() {
    val term = Term("internal").withOption("foo", "bar").withOption("baz", "quux")
    assertThat(term.toString()).isEqualTo("internal(baz=quux,foo=bar)")
  }

  @Test fun testToStringWithSpaces() {
    val term = Term("inte rnal").withOption("foo", "bar").withOption("baz", "qu ux")
    assertThat(term.toString()).isEqualTo("\"inte rnal\"(baz=\"qu ux\",foo=bar)")
  }
}
