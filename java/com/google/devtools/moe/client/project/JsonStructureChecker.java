/*
 * Copyright (c) 2016 Google, Inc.
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

package com.google.devtools.moe.client.project;

import com.google.common.base.Joiner;
import com.google.common.escape.SourceCodeEscapers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

/**
 * Checks that two GSON JsonElements are structurally equivalent.
 */
final class JsonStructureChecker {
  /** property name chain leading to the current element being processed. */
  private final Deque<String> keys = new ArrayDeque<>();

  static void requireSimilar(JsonElement a, JsonElement b) {
    new JsonStructureChecker().requireSimilarNoKey(a, b);
  }

  private void requireSimilarNoKey(JsonElement a, JsonElement b) {
    if (a.getClass() == b.getClass()) {
      if (a instanceof JsonArray) {
        Iterator<JsonElement> aEls = ((JsonArray) a).iterator();
        Iterator<JsonElement> bEls = ((JsonArray) b).iterator();
        int index = 0;
        for (; aEls.hasNext() && bEls.hasNext(); ++index) {
          requireSimilar(index, aEls.next(), bEls.next());
        }
        if (aEls.hasNext() || bEls.hasNext()) {
          addKey(index);
          dissimilar();
        }
        return;
      } else if (a instanceof JsonObject) {
        Iterator<Map.Entry<String, JsonElement>> aProps = ((JsonObject) a).entrySet().iterator();
        Iterator<Map.Entry<String, JsonElement>> bProps = ((JsonObject) b).entrySet().iterator();
        // We don't ignore order of properties.
        while (aProps.hasNext() && bProps.hasNext()) {
          Map.Entry<String, JsonElement> aEntry = aProps.next();
          Map.Entry<String, JsonElement> bEntry = bProps.next();
          String aKey = aEntry.getKey();
          if (aKey.equals(bEntry.getKey())) {
            requireSimilar(aKey, aEntry.getValue(), bEntry.getValue());
          } else {
            addKey(aKey);
            dissimilar();
          }
        }
        if (aProps.hasNext()) {
          addKey(aProps.next().getKey());
          dissimilar();
        } else if (bProps.hasNext()) {
          addKey(bProps.next().getKey());
          dissimilar();
        }
        return;
      } else if (a instanceof JsonPrimitive) {
        // Ignore all number differences.  There are a number of
        // kinds of differences we want to ignore.
        // 1. (1I, 1L, 1D) are similar enough.
        // 2. (-0, +0)
        // 3. Small differences in decimal decoding.
        if (((JsonPrimitive) a).isNumber()
            && ((JsonPrimitive) b).isNumber()) {
          return;
        }
      }
    }
    if (!a.equals(b)) {
      dissimilar();
    }
  }

  private void addKey(String k) {
    String humanReadableKey;
    if (isIdentifierName(k)) {
      humanReadableKey = "." + k;
    } else {
      humanReadableKey = "[\"" + SourceCodeEscapers.javascriptEscaper().escape(k) + "\"]";
    }
    keys.add(humanReadableKey);
  }

  private void addKey(int k) {
    keys.add("[" + k + "]");
  }

  /**
   * An EcmaScript IdentifierName is an Identifier or reserved word.
   * This is approximate to avoid bringing in extra dependencies.
   */
  private static boolean isIdentifierName(String k) {
    int n = k.length();
    if (n == 0) {
      return false;
    }
    if (!Character.isJavaIdentifierStart(k.charAt(0))) {
      return false;
    }
    for (int i = 1; i < n; ++i) {
      if (!Character.isJavaIdentifierPart(k.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private void requireSimilar(Object key, JsonElement a, JsonElement b) {
    if (key instanceof Integer) {
      addKey((Integer) key);
    } else {
      addKey(String.valueOf(key));
    }
    requireSimilarNoKey(a, b);
    keys.removeLast();
  }

  private void dissimilar() {
    String keyChain = Joiner.on("").join(keys);
    throw new InvalidProject(
        "MOE config uses problematic JavaScript constructs at "
        + "key chain " + keyChain + ".");
  }
}
