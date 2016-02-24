/*
 * Copyright (c) 2011 Google, Inc.
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

package com.google.devtools.moe.client;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import dagger.Provides;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Ui that outputs to {@code System.out} and {@code System.err}.
 */
@Singleton
public class SystemUi extends Ui {
  private static final Logger LOGGER = Logger.getLogger(SystemUi.class.getName());

  // We store the task that is the current output, if any, so that we can special case a Task that
  // is popped right after it is pushed. In this case, we can output: "Doing...Done" on one line.
  Task currentOutput;

  @Inject
  public SystemUi() {
    super();
    currentOutput = null;
  }

  /**
   * Clears the current output, if applicable.
   */
  private void clearOutput() {
    if (currentOutput != null) {
      // We're in the middle of a line, so start a new one.
      System.out.println();
    }
    currentOutput = null;
  }

  /**
   * Indents a message according to the STACK size.
   * 
   * @param message message to be indented.
   * @return the message indented.
   */
  private String indent(String message) {
    String indentation = Strings.repeat("  ", STACK.size());
    return indentation + Joiner.on("\n" + indentation).join(Splitter.on('\n').split(message));
  }

  @Override
  public void info(String message, Object... args) {
    clearOutput();
    logHelper(indent(String.format(message, args)));
  }

  @Override
  public void debug(String message, Object... args) {
    LOGGER.log(Level.INFO, String.format(message, args));
  }

  /**
   * Logs a message with {@link Level#INFO} level.
   * 
   * @param message message to be logged.
   */
  private void logHelper(String message) {
    System.out.println(message);
    LOGGER.log(Level.INFO, message);
  }

  @Override
  public void error(String messageFormat, Object... args) {
    clearOutput();
    LOGGER.log(Level.SEVERE, String.format(messageFormat, args));
  }

  @Override
  public void error(Throwable throwable, String messageFormat, Object... args) {
    clearOutput();
    String message = String.format(messageFormat, args);
    // Do not expose the STACK trace to the user. Just send it to the INFO logs.
    LOGGER.log(Level.SEVERE, message + ": " + throwable.getMessage());
    LOGGER.log(Level.INFO, message, throwable);
  }

  @Override
  public Task pushTask(String task, String descriptionFormat, Object... args) {
    clearOutput();
    String description = String.format(descriptionFormat, args);
    String indented = indent(description + "... ");
    System.out.print(indented);
    LOGGER.log(Level.INFO, indented);

    currentOutput = super.pushTask(task, description);
    return currentOutput;
  }

  @Override
  public void popTask(Task task, String result) {
    super.popTask(task, result);
    if (result.isEmpty()) {
      result = "Done";
    }
    if (currentOutput == task) {
      // The last thing we printed was starting this task
      logHelper(result);
    } else {
      // We need to print the description again
      logHelper(indent("DONE: " + task.description + ": " + result));
    }
    currentOutput = null;
  }

  /**
   * A Dagger module for binding this implementation of {@link Ui}.
   */
  @dagger.Module
  public static class Module {
    @Provides
    public Ui ui(SystemUi impl) {
      return impl;
    }
  }
}
