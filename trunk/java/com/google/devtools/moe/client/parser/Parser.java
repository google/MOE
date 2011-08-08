// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.StringReader;
import java.io.StreamTokenizer;
import java.lang.IllegalArgumentException;
import java.util.List;
import java.util.Map;

/**
 * Parser for Terms; useful for various Expression Languages
 *
 * This allows MOE to embed options in one string instead of having many
 * orthogonal flags.
 *
 * The syntax for a Term is:
 *
 * TERM -> LITERAL OPTIONS
 * OPTIONS ->
 * OPTIONS -> (OPTIONLIST)
 * OPTIONLIST -> OPTION
 * OPTIONLIST ->
 * OPTIONLIST -> OPTIONLIST , OPTION
 * OPTION -> LITERAL = LITERAL
 * LITERAL -> alphanumeric+
 * LITERAL -> "[^"]"*
 *
 * Examples:
 *
 * internal
 * internal(revision=45)
 * public
 * file(path="/path/to/public.tar")
 *
 * Users should use the high-level functions:
 * parseOperator
 * parseTerm
 * parseTermCompletely
 * tokenize
 *
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Parser {

  /**
   * An error occurred while parsing.
   */
  public static class ParseError extends Exception {
    public static String error;

    public ParseError(String error) {
      this.error = error;
    }

    public String getMessage() {
      return String.format("Cannot parse: %s", error);
    }
  }

  static class ParseOptionResult {
    public String key;
    public String value;

    public ParseOptionResult(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  static ParseOptionResult parseOption(StreamTokenizer input) throws ParseError {
    try {

      int result = input.nextToken();
      if (result != StreamTokenizer.TT_WORD && result != '"') {
        throw new ParseError("expected word during option key parse: " + input);
      }
      String key = input.sval;

      result = input.nextToken();
      if (result != '=') {
        throw new ParseError("key and value in option must be separated by \"=\":" + input);
      }

      result = input.nextToken();
      if (result != StreamTokenizer.TT_WORD && result != '"') {
        throw new ParseError("expected word during option value parse" + input);
      }
      String value = input.sval;

      return new ParseOptionResult(key, value);
    } catch (IOException e) {
      throw new ParseError(e.getMessage());
    }
  }

  static Map<String, String> parseOptions(StreamTokenizer input) throws ParseError {
    try {
      int result = input.nextToken();
      if (result == StreamTokenizer.TT_EOF) {
        return ImmutableMap.<String, String>of();
      }
      if (result != '(') {
        input.pushBack();
        return ImmutableMap.<String, String>of();
      }

      ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
      while (true) {
        result = input.nextToken();
        if (result == StreamTokenizer.TT_EOF) {
          throw new ParseError("options not terminated by \")\"");
        }

        if (result == ')') {
          return builder.build();
        }

        input.pushBack();
        ParseOptionResult r = parseOption(input);
        builder.put(r.key, r.value);

        result = input.nextToken();
        if (result == ')') {
          return builder.build();
        }

        if (result != ',') {
          throw new ParseError("text after option must be \",\" or \")\"");
        }
      }
    } catch (IOException e) {
      throw new ParseError(e.getMessage());
    }
  }

  /**
   * Parses a Term.
   *
   * @param input  text to parse
   *
   * @return the parsed term
   */
  static public Term parseTerm(StreamTokenizer input) throws ParseError {
    try {
      int result = input.nextToken();
      if (result != StreamTokenizer.TT_WORD && result != '"') {
        throw new ParseError("expected word during identifier parse" + input);
      }
      String identifier = input.sval;

      Map<String, String> options = parseOptions(input);
      return new Term(identifier, options);
    } catch (IOException e) {
      throw new ParseError(e.getMessage());
    }
  }

  /**
   * Checks that the input is exhauasted.
   *
   * @param input  the input tokenizer
   *
   * @throws ParseError if input remains
   */
  static public void checkInputExhausted(StreamTokenizer input) throws ParseError{
    try {
      if (input.nextToken() != StreamTokenizer.TT_EOF) {
        throw new ParseError("unexpected text after expression: " + input);
      }
    } catch (IOException e) {
      throw new ParseError(e.getMessage());
    }
  }

  /**
   * Determines if the input is exhausted.
   *
   * @param
   */
  static public boolean isInputExhausted(StreamTokenizer input) throws ParseError {
    try {
      if (input.nextToken() == StreamTokenizer.TT_EOF) {
        return true;
      }
      input.pushBack();
      return false;
    } catch (IOException e) {
      throw new ParseError(e.getMessage());
    }
  }

  /**
   * Parses a Term from input, throwing an error if the input contains more than a Term.
   *
   * @param input  text to parse
   *
   * @return the parsed term
   * @throws ParseError if the string is not exactly a Term
   */
  static public Term parseTermCompletely(String input) throws ParseError {
    StreamTokenizer t = tokenize(input);
    Term result = parseTerm(t);
    checkInputExhausted(t);
    return result;
  }

  static public Operator parseOperator(StreamTokenizer input) throws ParseError {
    try {
      int operator = input.nextToken();
      try {
        Operator result = Operator.getOperator((char) operator);
        return result;
      } catch (IllegalArgumentException e) {
        throw new ParseError(
            String.format("Invalid operator \"%s\"", input.toString()));
      }
    } catch (IOException e) {
      throw new ParseError(e.getMessage());
    }
  }

  static public List<Operation> parseOperationList(StreamTokenizer input) throws ParseError {
    ImmutableList.Builder<Operation> operations = new ImmutableList.Builder<Operation>();
    while(!Parser.isInputExhausted(input)) {
      Operator operator = parseOperator(input);
      Term t = Parser.parseTerm(input);
      operations.add(new Operation(operator, t));
   }
    return operations.build();
  }

  static public StreamTokenizer tokenize(String input) {
    StreamTokenizer result =  new StreamTokenizer(new StringReader(input));
    result.resetSyntax();
    result.wordChars('a', 'z');
    result.wordChars('A', 'Z');
    result.wordChars('0', '9');
    result.whitespaceChars(0, 32);
    result.quoteChar('"');

    return result;
  }
}
