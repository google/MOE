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
  private final Logger logger = Logger.getLogger(SystemUi.class.getName());

  // We store the task that is the current output, if any, so that we can special case a Task that
  // is popped right after it is pushed. In this case, we can output: "Doing...Done" on one line.
  Ui.Task currentOutput;

  @Inject
  public SystemUi(FileSystem fileSystem) {
    super(fileSystem);
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

  private String indent(String msg) {
    String indentation = Strings.repeat("  ", stack.size());
    return indentation + Joiner.on("\n" + indentation).join(Splitter.on('\n').split(msg));
  }

  @Override
  public void info(String msg, Object... args) {
    clearOutput();
    logHelper(indent(String.format(msg, args)));
  }

  @Override
  public void debug(String msg, Object... args) {
    logger.log(Level.INFO, String.format(msg, args));
  }

  private void logHelper(String message) {
    System.out.println(message);
    logger.log(Level.INFO, message);
  }

  @Override
  public void error(String msg, Object... args) {
    clearOutput();
    logger.log(Level.SEVERE, String.format(msg, args));
  }

  @Override
  public void error(Throwable e, String msg, Object... args) {
    clearOutput();
    String message = String.format(msg, args);
    // Do not expose the stack trace to the user. Just send it to the INFO logs.
    logger.log(Level.SEVERE, message + ": " + e.getMessage());
    logger.log(Level.INFO, message, e);
  }

  @Override
  public Ui.Task pushTask(String task, String descriptionFormat, Object... args) {
    clearOutput();
    String description = String.format(descriptionFormat, args);
    String indented = indent(description + "... ");
    System.out.print(indented);
    logger.log(Level.INFO, indented);

    currentOutput = super.pushTask(task, description);
    return currentOutput;
  }

  @Override
  public void popTask(Ui.Task task, String result) {
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

  /** A Dagger module for binding this implementation of {@link Ui}. */
  @dagger.Module
  public static class Module {
    @Provides
    public Ui ui(SystemUi impl) {
      return impl;
    }
  }
}
