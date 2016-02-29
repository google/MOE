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

package com.google.devtools.moe.client.parser;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;

import java.util.Collections;
import java.util.Map;

/**
 * A Term in the MOE Expression Language.
 *
 * <p>An identifier with optional parameters. E.g., "foo" or "internal(revision=45)"
 */
public class Term {

  public final String identifier;
  public final Map<String, String> options;

  public Term(String identifier, Map<String, String> options) {
    this.identifier = identifier;
    this.options = Collections.unmodifiableMap(options);
  }

  /**
   * Add an option name-value pair to the Term, e.g. "myRepo" -> "myRepo(revision=4)".
   */
  public Term withOption(String optionName, String optionValue) {
    Map<String, String> newOptions =
        ImmutableMap.<String, String>builder().putAll(options).put(optionName, optionValue).build();
    return new Term(identifier, newOptions);
  }

  private static String quote(String s) {
    if (CharMatcher.javaLetterOrDigit().matchesAllOf(s)) {
      return s;
    }
    return "\"" + s + "\"";
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(quote(identifier));
    if (!options.isEmpty()) {
      result.append("(");
      // We sort so that toString() is deterministic.
      for (String s : ImmutableSortedSet.copyOf(options.keySet())) {
        result.append(quote(s));
        result.append("=");
        result.append(quote(options.get(s)));
        result.append(",");
      }
      // strip off last comma
      result.deleteCharAt(result.length() - 1);
      result.append(")");
    }
    return result.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Term)) {
      return false;
    }
    return toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
