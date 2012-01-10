// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ui that outputs to System.out and System.err
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SystemUi extends Ui {
  private final Logger logger =
      Logger.getLogger(SystemUi.class.getName());

  // We store the task that is the current output, if any, so that we can special case a Task that
  // is popped right after it is pushed. In this case, we can output: "Doing...Done" on one line.
  Ui.Task currentOutput;

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

  private String indent(String msg) {
    String indentation = Strings.repeat("  ", stack.size());
    return indentation + Joiner.on("\n" + indentation).join(Splitter.on('\n').split(msg));
  }

  @Override
  public void info(String msg) {
    clearOutput();
    logHelper(indent(msg));
  }

  public void debug(String msg) {
    logger.log(Level.INFO, msg);
  }

  private void logHelper(String msg) {
    System.out.println(msg);
    logger.log(Level.INFO, msg);
  }

  @Override public void error(String msg) {
    clearOutput();
    logger.log(Level.SEVERE, msg);
  }

  @Override public void error(Throwable e, String msg) {
    clearOutput();

    // Do not expose the stack trace to the user. Just send it to the INFO logs.
    logger.log(Level.SEVERE, msg + ": " + e.getMessage());
    logger.log(Level.INFO, msg, e);
  }

  @Override
  public Ui.Task pushTask(String task, String description) {
    clearOutput();

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
      logHelper(
          indent("DONE: " + task.description + ": " + result));
    }
    currentOutput = null;
  }
}
