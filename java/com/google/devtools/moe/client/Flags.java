// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.collect.Lists;
import com.google.devtools.moe.client.options.MoeOptions;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code to parse flags.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Flags {
  private static final Logger logger = Logger.getLogger(MoeOptions.class.getName());

  /**
   * Parses options from args. Mutates this object. Prints help and exits if it should.
   */
  public static void parseOptions(MoeOptions options, List<String> args) {
    CmdLineParser parser = new CmdLineParser(options);
    boolean parseError = false;
    try {
      parser.parseArgument(processArgs(args).toArray(new String[] {}));
    } catch (CmdLineException e) {
      logger.log(Level.SEVERE, "Failure", e);
      parseError = true;
    }

    if (options.shouldDisplayHelp() || parseError) {
      parser.printUsage(System.err);
      System.exit(parseError ? 1 : 0);
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
