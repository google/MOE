// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.collect.Lists;
import com.google.devtools.moe.client.directives.Directive;
import com.google.devtools.moe.client.directives.DirectiveFactory;
import com.google.devtools.moe.client.directives.MoeOptions;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MOE (Make Open Easy) client.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Moe {

  private Moe() {}

  public static void main(String[] args) {
    AppContext.init();

    if (args.length < 1) {
      System.err.println("Usage: moe <directive>");
      System.exit(1);
    }

    Directive d = DirectiveFactory.makeDirective(args[0]);
    if (d == null) {
      System.exit(1);
    }

    MoeOptions flags = d.getFlags();
    CmdLineParser parser = new CmdLineParser(flags);
    boolean parseError = false;
    try {
      List<String> nextArgs = Lists.newArrayList(args);
      nextArgs.remove(0); // Remove the directive.
      parser.parseArgument(
          processArgs(nextArgs).toArray(new String[] {}));
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parseError = true;
    }

    if (flags.shouldDisplayHelp() || parseError) {
      parser.printUsage(System.err);
      System.exit(parseError ? 1 : 0);
    }
    try {
      System.exit(d.perform());
    } catch (MoeProblem m) {
      // TODO(dbentley): have an option for verbosity; if it is above a threshold, print
      // a stack trace.
      AppContext.RUN.ui.error(m.explanation);
      AppContext.RUN.ui.error("Moe encountered a problem; look above for explanation");
      System.exit(1);
    }
  }

  private static List<String> processArgs(List<String> args) {
    // Args4j has a different format that the old command-line parser.
    // So we use some voodoo to get the args into the format that args4j
    // expects.
    Pattern argPattern = Pattern.compile("(--[a-zA-Z_]+)=(.*)");
    Pattern quotesPattern = Pattern.compile("^['\"](.*)['\"]$");
    List<String> processedArgs = Lists.newArrayList();

    for (String arg : args) {
      Matcher matcher = argPattern.matcher(arg);
      if (matcher.matches()) {
        processedArgs.add(matcher.group(1));

        String value = matcher.group(2);
        Matcher quotesMatcher = quotesPattern.matcher(value);
        if (quotesMatcher.matches()) {
          processedArgs.add(quotesMatcher.group(1));
        } else {
          processedArgs.add(value);
        }
      } else {
        processedArgs.add(arg);
      }
    }

    return processedArgs;
  }
}
