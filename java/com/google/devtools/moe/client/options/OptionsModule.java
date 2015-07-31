// Copyright 2015 The MOE Authors All Rights Reserved.
package com.google.devtools.moe.client.options;

import com.google.common.base.Preconditions;
import com.google.devtools.moe.client.directives.Directives.SelectedDirective;

import dagger.Provides;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

/** Dagger module to seed command-line arguments into the graph */
@dagger.Module
public class OptionsModule {
  private String[] rawArgs;

  public OptionsModule(String[] args) {
    Preconditions.checkArgument(
        args.length > 0, OptionsModule.class.getSimpleName() + " requires a non-empty args list.");
    this.rawArgs = args;
  }

  @Provides
  @SelectedDirective
  String selectedDirective() {
    return rawArgs[0];
  }

  @Provides
  @Singleton // No need to parse arguments several times.
  String[] provideArgs() {
    return preProcessArgs(rawArgs);
  }

  /**
   * Preprocesses command line arguments to make them conform to args4j assumptions (for backwards
   * compability) and strip off the directive.
   */
  private static String[] preProcessArgs(String[] unprocessedArgs) {
    List<String> args = new LinkedList<>();
    Collections.addAll(args, unprocessedArgs);
    args.remove(0); // Remove the directive.
    // Args4j has a different format than the old command-line parser.
    // So we use some voodoo to get the args into the format that args4j
    // expects.
    Pattern argPattern = Pattern.compile("(--[a-zA-Z_]+)=(.*)");
    // TODO(cgruber): Look at these patterns
    Pattern quotesPattern = Pattern.compile("^['\"](.*)['\"]$");
    List<String> processedArgs = new ArrayList<>(args.size());
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
    return processedArgs.toArray(new String[args.size()]);
  }
}
