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

import com.google.common.base.CharMatcher
import com.google.common.base.Joiner
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSortedMap

/**
 * A Term in the MOE Expression Language.
 **
 * An identifier with optional parameters. E.g., `foo` or `internal(revision=45)`
 */
data class Term @JvmOverloads constructor(
  var identifier: String,
  var options: ImmutableSortedMap<String, String> = ImmutableSortedMap.of()
) {

  /**
   * Add an option name-value pair to the Term, e.g. "myRepo" -> "myRepo(revision=4)".
   */
  fun withOption(optionName: String, optionValue: String): Term {
    return Term(
        identifier,
        ImmutableSortedMap.naturalOrder<String, String>()
            .putAll(options)
            .put(optionName, optionValue)
            .build())
  }

  /** Add multiple name-value pairs to the Term, e.g. "myRepo" -> "myRepo(revision=4, bar=blah)".  */
  fun withOptions(moreOptions: Map<String, String>): Term {
    return Term(
        identifier,
        ImmutableSortedMap.naturalOrder<String, String>()
            .putAll(options)
            .putAll(moreOptions)
            .build())
  }

  private fun maybeQuote(string: String): String {
    return if (CharMatcher.javaLetterOrDigit().matchesAllOf(string)) string else "\"${string}\""
  }

  override fun toString(): String {
    val quotedMap = ImmutableMap.builder<String, String>()
    for (entry in options.entries) {
      quotedMap.put(maybeQuote(entry.key), maybeQuote(entry.value))
    }
    val result = StringBuilder()
    result.append(maybeQuote(identifier))
    if (!options.isEmpty()) {
      result.append("(")
      Joiner.on(',').withKeyValueSeparator('=').appendTo(result, quotedMap.build())
      result.append(")")
    }
    return result.toString()
  }
}
