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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.directives.Directives.SelectedDirective;

import dagger.Provides;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Qualifier;
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
  @Nullable
  @Argument("config_file")
  static String configFile(String... args) {
    // TODO(cgruber) Migrate to JCommander, so we don't have to manually parse some of these.
    List<String> matchingArgs = ImmutableList.of("-c", "--config", "--config_file");
    for (int i = 0; i < args.length; i++) {
      if (matchingArgs.contains(args[i])) {
        if ((i + 1) >= args.length) {
          throw new IllegalArgumentException("'" + args[i] + "' specified without a parameter");
        }
        if (args[i + 1].startsWith("-")) {
          throw new IllegalArgumentException("'" + args[i] + "' is not followed by a path");
        }
        return args[i + 1];
      }
      // check for "--config=" style.
      for (String prefix : matchingArgs) {
        if (args[i].startsWith(prefix + "=")) {
          return args[i].substring(prefix.length() + 1);
        }
      }
    }
    // Some commands may not require config
    // TODO(cgruber) make this not-nullable when only config-requiring commands yank in the config.
    return null;
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

  /**
   * A JSR-330 {@link Qualifier} annotation to distinguish injected argument values from other
   * injected {@link String} values.
   */
  @Qualifier
  public @interface Argument {
    String value();
  }
}
