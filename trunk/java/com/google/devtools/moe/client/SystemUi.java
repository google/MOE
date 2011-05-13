// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * Ui that outputs to System.out and System.err
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SystemUi extends Ui {

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

  public void info(String msg) {
    clearOutput();
    System.out.println(indent(msg));
  }

  public void error(String msg) {
    clearOutput();
    System.err.println(String.format("ERROR: %s", msg));
  }

  public Ui.Task pushTask(String task, String description) {
    clearOutput();
    System.out.print(indent(description + "... "));
    currentOutput = super.pushTask(task, description);
    return currentOutput;
  }

  public void popTask(Ui.Task task, String result) {
    super.popTask(task, result);
    if (result.isEmpty()) {
      result = "Done";
    }
    if (currentOutput == task) {
      // The last thing we printed was starting this task
      System.out.println(result);
    } else {
      // We need to print the description again
      System.out.println(indent("DONE: " + task.description + ": " + result));
    }
    currentOutput = null;
  }
}
