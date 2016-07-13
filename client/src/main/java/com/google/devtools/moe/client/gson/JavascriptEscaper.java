/*
 * Copyright (C) 2009 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.devtools.moe.client.gson;

import com.google.common.collect.ImmutableMap;
import com.google.common.escape.ArrayBasedCharEscaper;
import com.google.common.escape.Escaper;

/**
 * Keeps an Escaper instance used to escape strings for safe use in various common programming
 * languages.
 */
final class JavascriptEscaper {
  private JavascriptEscaper() {}

  // From: http://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters
  private static final char PRINTABLE_ASCII_MIN = 0x20; // ' '
  private static final char PRINTABLE_ASCII_MAX = 0x7E; // '~'

  private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

  static final Escaper JAVASCRIPT_ESCAPER;

  static {
    ImmutableMap.Builder<Character, String> jsMap = ImmutableMap.builder();
    jsMap.put('\'', "\\x27");
    jsMap.put('"', "\\x22");
    jsMap.put('<', "\\x3c");
    jsMap.put('=', "\\x3d");
    jsMap.put('>', "\\x3e");
    jsMap.put('&', "\\x26");
    jsMap.put('\b', "\\b");
    jsMap.put('\t', "\\t");
    jsMap.put('\n', "\\n");
    jsMap.put('\f', "\\f");
    jsMap.put('\r', "\\r");
    jsMap.put('\\', "\\\\");
    JAVASCRIPT_ESCAPER =
        new ArrayBasedCharEscaper(jsMap.build(), PRINTABLE_ASCII_MIN, PRINTABLE_ASCII_MAX) {
          @Override
          protected char[] escapeUnsafe(char c) {
            // Do two digit hex escape for value less than 0x100.
            if (c < 0x100) {
              char[] r = new char[4];
              r[3] = HEX_DIGITS[c & 0xF];
              c >>>= 4;
              r[2] = HEX_DIGITS[c & 0xF];
              r[1] = 'x';
              r[0] = '\\';
              return r;
            }
            return asUnicodeHexEscape(c);
          }
        };
  }

  /**
   * Turns all non-ASCII characters into ASCII javascript escape sequences.
   */
  static String escape(String s) {
    return JAVASCRIPT_ESCAPER.escape(s);
  }

  private static char[] asUnicodeHexEscape(char c) {
    // Equivalent to String.format("\\u%04x", (int) c);
    char[] r = new char[6];
    r[0] = '\\';
    r[1] = 'u';
    r[5] = HEX_DIGITS[c & 0xF];
    c >>>= 4;
    r[4] = HEX_DIGITS[c & 0xF];
    c >>>= 4;
    r[3] = HEX_DIGITS[c & 0xF];
    c >>>= 4;
    r[2] = HEX_DIGITS[c & 0xF];
    return r;
  }
}
