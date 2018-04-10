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

package com.google.devtools.moe.client.codebase.expressions;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A Term in the MOE Expression Language.
 *
 * <p>An identifier with optional parameters. E.g., "foo" or "internal(revision=45)"
 */
@AutoValue
public abstract class Term {

  public abstract String identifier();

  public abstract ImmutableSortedMap<String, String> options();

  /**
   * Add an option name-value pair to the Term, e.g. "myRepo" -> "myRepo(revision=4)".
   */
  public Term withOption(String optionName, String optionValue) {
    return new AutoValue_Term(
        identifier(),
        ImmutableSortedMap.<String, String>naturalOrder()
            .putAll(options())
            .put(optionName, optionValue)
            .build());
  }

  /** Add multiple name-value pairs to the Term, e.g. "myRepo" -> "myRepo(revision=4, bar=blah)". */
  public Term withOptions(Map<String, String> moreOptions) {
    return new AutoValue_Term(
        identifier(),
        ImmutableSortedMap.<String, String>naturalOrder()
            .putAll(options())
            .putAll(moreOptions)
            .build());
  }

  private static String maybeQuote(String s) {
    if (CharMatcher.javaLetterOrDigit().matchesAllOf(s)) {
      return s;
    }
    return "\"" + s + "\"";
  }

  @Memoized
  @Override
  public String toString() {
    ImmutableMap.Builder<String, String> quotedMap = ImmutableMap.builder();
    for (Entry<String, String> entry : options().entrySet()) {
      quotedMap.put(maybeQuote(entry.getKey()), maybeQuote(entry.getValue()));
    }
    StringBuilder result = new StringBuilder();
    result.append(maybeQuote(identifier()));
    if (!options().isEmpty()) {
      result.append("(");
      Joiner.on(',').withKeyValueSeparator('=').appendTo(result, quotedMap.build());
      result.append(")");
    }
    return result.toString();
  }

  public static Term create(String identifier) {
    return new AutoValue_Term(identifier, ImmutableSortedMap.of());
  }
}
