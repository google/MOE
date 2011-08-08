// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.parser;

import static com.google.devtools.moe.client.parser.Parser.tokenize;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ParserTest extends TestCase {

  void assertOptionFail(String s) {
    try {
      Parser.parseOption(tokenize(s));
      fail("Successfully parsed invalid string: " + s);
    } catch (Exception e) {}
  }

  public void testParseOption() throws Exception {
    Parser.ParseOptionResult r;

    r = Parser.parseOption(tokenize("key=value baz"));
    assertEquals("key", r.key);
    assertEquals("value", r.value);
    r = Parser.parseOption(tokenize("key    = value baz"));
    assertEquals("key", r.key);
    assertEquals("value", r.value);
    r = Parser.parseOption(tokenize("\"key\" = \"value\" baz"));
    assertEquals("key", r.key);
    assertEquals("value", r.value);

    assertOptionFail("key=");
    assertOptionFail("=value");
    assertOptionFail("key");
    assertOptionFail("key value");
    assertOptionFail(" = ");
  }

  void assertOptionsFail(String s) {
    try {
      Parser.parseOption(tokenize(s));
      fail("Successfully parsed invalid string: " + s);
    } catch (Parser.ParseError e) {}
  }

  public void assertOptionsResult(String s, ImmutableMap<String, String> expected) {
    try {
      Map<String, String> actual = Parser.parseOptions(tokenize(s));
      assertEquals(expected, actual);
    } catch (Parser.ParseError e) {
      fail(String.format("Could not parse %s: %s", s, e));
    }
  }

  public void testParseOptions() throws Exception {
    Map<String, String> r;

    assertOptionsResult("", ImmutableMap.<String, String>of());
    assertOptionsResult(">public", ImmutableMap.<String, String>of());
    assertOptionsResult("()>public", ImmutableMap.<String, String>of());
    assertOptionsResult("(revision=45)>public", ImmutableMap.of("revision", "45"));
    assertOptionsResult("(  revision = 45  ,  2=3) >public",
                        ImmutableMap.of("revision", "45", "2", "3"));
    assertOptionsResult("key=value", ImmutableMap.<String, String>of());

    assertOptionsFail("(");
    assertOptionsFail("(key=value");
    assertOptionsFail("(key=value,key2=value2");
    assertOptionsFail("(a=b c=d)");
    assertOptionsFail("(key=value,)");
  }

  public void assertParseTermCompletely(String input, String identifier,
                                        ImmutableMap<String, String> options)
      throws Parser.ParseError{
    Term r = Parser.parseTermCompletely(input);
    assertEquals(identifier, r.identifier);
    assertEquals(options, r.options);
  }

  public void assertParseTermCompletelyFails(
      String input, String errorMessage) {
    try {
      Term r = Parser.parseTermCompletely(input);
      fail(String.format("Successfully parsed invalid string: %s into %s and %s",
                         input, r.identifier, r.options.toString()));
    } catch (Parser.ParseError e) {
      assertEquals(errorMessage, e.error);
    }
  }

  public void testParseTermCompletely() throws Exception {
    assertParseTermCompletely("internal", "internal", ImmutableMap.<String, String>of());
    assertParseTermCompletely("    internal    ", "internal", ImmutableMap.<String, String>of());
    assertParseTermCompletely("    internal () ", "internal", ImmutableMap.<String, String>of());
    assertParseTermCompletely(" internal (foo=bar) ",
                              "internal", ImmutableMap.of("foo", "bar"));
    assertParseTermCompletely("internal(foo=bar,baz=quux)",
                              "internal", ImmutableMap.of("foo", "bar", "baz", "quux"));
    assertParseTermCompletely("internal(foo=bar, baz=quux)",
                              "internal", ImmutableMap.of("foo", "bar", "baz", "quux"));

    assertParseTermCompletelyFails("",
        "expected word during identifier parseToken[EOF], line 1");
    assertParseTermCompletelyFails(" ",
        "expected word during identifier parseToken[EOF], line 1");
    assertParseTermCompletelyFails("internal(",
        "options not terminated by \")\"");
    assertParseTermCompletelyFails("internal)",
        "unexpected text after expression: Token[')'], line 1");
    assertParseTermCompletelyFails("internal(a=b c=d)",
        "text after option must be \",\" or \")\"");
    assertParseTermCompletelyFails("internal foo",
        "unexpected text after expression: Token[foo], line 1");
  }

  public void assertParseTerm(String input, String identifier,
                              ImmutableMap<String, String> options) throws Parser.ParseError {
    Term r = Parser.parseTerm(Parser.tokenize(input));
    assertEquals(identifier, r.identifier);
    assertEquals(options, r.options);
  }

  public void assertParseTermFails(String input, String errorMessage) {
    try {
      Term r = Parser.parseTerm(Parser.tokenize(input));
      fail(String.format("Successfully parsed invalid string: %s into %s and %s",
                         input, r.identifier, r.options.toString()));
    } catch (Parser.ParseError e) {
      assertEquals(errorMessage, e.error);
    }
  }

  public void testParseTerm() throws Exception {
    assertParseTerm("internal", "internal", ImmutableMap.<String, String>of());
    assertParseTerm("    internal    ", "internal", ImmutableMap.<String, String>of());
    assertParseTerm("    internal () ", "internal", ImmutableMap.<String, String>of());
    assertParseTerm(" internal (foo=bar) ",
                    "internal", ImmutableMap.of("foo", "bar"));
    assertParseTerm("internal(foo=bar,baz=quux)",
                    "internal", ImmutableMap.of("foo", "bar", "baz", "quux"));
    assertParseTerm("internal(foo=bar, baz=quux)",
                    "internal", ImmutableMap.of("foo", "bar", "baz", "quux"));
    assertParseTerm("internal(foo=bar, baz=quux) foo",
                    "internal", ImmutableMap.of("foo", "bar", "baz", "quux"));
    // Yes, this looks weird. It parses "internal", and has a remaining ")".
    // So the error will be thrown next time we try to parse anything, or check that it's exhausted.
    assertParseTerm("internal)", "internal", ImmutableMap.<String, String>of());

    assertParseTermFails("",
        "expected word during identifier parseToken[EOF], line 1");
    assertParseTermFails(" ",
        "expected word during identifier parseToken[EOF], line 1");
    assertParseTermFails("internal(",
        "options not terminated by \")\"");
    assertParseTermFails("internal(a=b c=d)",
        "text after option must be \",\" or \")\"");
  }

  public void testParsePipe() throws Exception {
    assertEquals(Operator.EDIT, Parser.parseOperator(Parser.tokenize("|")));
    assertEquals(Operator.TRANSLATE, Parser.parseOperator(Parser.tokenize(">")));

    assertParseOperatorFails("a",
        "Invalid operator \"Token[a], line 1\"");
    assertParseOperatorFails("-",
        "Invalid operator \"Token['-'], line 1\"");
  }

  public void assertParseOperatorFails(String input, String errorMessage) {
    try {
      Parser.parseOperator(Parser.tokenize(input));
      fail();
    } catch (Parser.ParseError p) {
      assertEquals(errorMessage, p.error);
    }
  }


  public void assertOperationListRoundTrip(String expected, String input) throws Exception {
    List<Operation> terms = Parser.parseOperationList(tokenize(input));
    StringBuilder r = new StringBuilder();
    for (Operation op: terms) {
      r.append(op.operator);
      r.append(op.term);
    }
    assertEquals(expected, r.toString());
  }

  public void testParseOperationList() throws Exception {
    assertOperationListRoundTrip("|foo|bar", "|foo|bar");
    assertOperationListRoundTrip("|foo|bar", "| foo  |  bar");
    assertOperationListRoundTrip("|foo(baz=quux)|bar", "|foo(baz=quux)|bar");
    assertOperationListRoundTrip(">public|editor", ">public|editor");
 }

}
