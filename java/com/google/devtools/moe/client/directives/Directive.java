// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.collect.Lists;
import com.google.devtools.moe.client.MoeOptions;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Directive is what MOE should do in this run.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public abstract class Directive {
  private static final Logger logger = Logger.getLogger(Directive.class.getName());

  /**
   * Performs the Directive's work.
   *
   * @return the status of performing the Directive, suitable for returning from this process.
   */
  public abstract int perform();

  /**
   * Gets the flags for this directive. Will be auto-populated by args4j reflection.
   */
  public abstract MoeOptions getFlags();

  /**
   * Get description suitable for command-line help.
   */
  public abstract String getDescription();

  /**
   * Parses command-line flags, returning true if the parse was successful and no flags errors were
   * found.
   */
  public boolean parseFlags(String[] args) {
    CmdLineParser parser = new CmdLineParser(getFlags());
    try {
      List<String> nextArgs = Lists.newArrayList(args);
      nextArgs.remove(0); // Remove the directive.
      parser.parseArgument(processArgs(nextArgs).toArray(new String[] {}));
      if (getFlags().shouldDisplayHelp()) {
        parser.printUsage(System.err);
      }
      return true;
    } catch (CmdLineException e) {
      logger.log(Level.SEVERE, "Failure", e);
      parser.printUsage(System.err);
      return false;
    }
  }

  private static List<String> processArgs(List<String> args) {
    // Args4j has a different format than the old command-line parser.
    // So we use some voodoo to get the args into the format that args4j
    // expects.
    Pattern argPattern = Pattern.compile("(--[a-zA-Z_]+)=(.*)");
    // TODO(cgruber): Look at these patterns
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
