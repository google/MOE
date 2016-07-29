/*
 * Copyright (c) 2015 Google, Inc.
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
package com.google.devtools.moe.client.options;

import com.google.devtools.moe.client.directives.Directive;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * Performs options/flags parsing.
 */
@Singleton
public class OptionsParser {
  private static final Logger logger = Logger.getLogger(OptionsParser.class.getName());

  private final String[] preprocessedArgs;
  private final boolean debug;

  /**
   * Creates the OptionsParser
   *
   * @param preprocessedArgs  An array of command-line arguments expected to have had the
   *     initial command/directive removed.
   * */
  @Inject
  OptionsParser(String[] preprocessedArgs) {
    this.preprocessedArgs = preprocessedArgs;
    this.debug = debugFlagPresent(preprocessedArgs);
  }

  /**
   * An ultra-thin pre-options-parser to check for the debug flag, in order to allow debug logging
   * before the graph (and options-parser) is initialized.
   */
  public static boolean debugFlagPresent(String[] preprocessedArgs) {
    return Arrays.asList(preprocessedArgs).contains("--debug");
  }

  /**
   * An ultra-thin pre-options-parser to check for the help flag, in order to allow objects to be
   * initialized to no-op implementations during {@link Directive} creation, since that all has to
   * happen before the instance can be passed to args processing, to generate the help output.
   */
  // TODO(cgruber) Remove this whole structure if we use JCommander and subsidiary args objects.
  public static boolean helpFlagPresent(String[] preprocessedArgs) {
    return Arrays.asList(preprocessedArgs).contains("--help");
  }

  public boolean debug() {
    return debug;
  }

  /**
   * Parses command-line flags, returning true if the parse was successful and no flags errors were
   * found.
   */
  // TODO(cgruber) Rework this with JCommander so we can have plugin-specific flags
  public boolean parseFlags(MoeOptions options) {
    CmdLineParser parser = new CmdLineParser(options);
    try {
      parser.parseArgument(preprocessedArgs);
      if (options.shouldDisplayHelp()) {
        parser.printUsage(System.err);
      }
      return true;
    } catch (CmdLineException e) {
      logger.log(Level.SEVERE, e.getMessage());
      parser.printUsage(System.err);
      return false;
    }
  }
}
