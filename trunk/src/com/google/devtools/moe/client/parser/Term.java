// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.parser;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSortedSet;

import java.lang.StringBuilder;
import java.util.Collections;
import java.util.Map;

/**
 * A Term in the MOE Expression Language.
 *
 * An identifier with optional parameters. E.g., "foo" or "internal(revision=45)"
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Term {

  public final String identifier;
  public final Map<String, String> options;

  public Term(String identifier, Map<String, String> options) {
    this.identifier = identifier;
    this.options = Collections.unmodifiableMap(options);
  }

  private static String quote(String s) {
    if (CharMatcher.JAVA_LETTER_OR_DIGIT.matchesAllOf(s)) {
      return s;
    }
    return "\"" + s + "\"";
  }

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
      result.deleteCharAt(result.length()-1);
      result.append(")");
    }
    return result.toString();
  }

  public boolean equals(Object o) {
    if (!(o instanceof Term)) {
      return false;
    }
    return toString().equals(o.toString());
  }

}
