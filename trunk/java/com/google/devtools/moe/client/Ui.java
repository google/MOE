// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * User Interface interface for MOE.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public abstract class Ui {

  protected final Deque<Task> stack;

  public Ui() {
    stack = new ArrayDeque<Task>();
  }

  /**
   * Sends an informational message to the user.
   *
   * @param msg  the informational message.
   */
  public abstract void info(String msg);

  /**
   * Reports an error to the user.
   *
   * @param msg  the error message.
   */
  public abstract void error(String msg);

  public static class Task {
    public final String taskName;
    public final String description;

    public Task(String taskName, String description) {
      this.taskName = taskName;
      this.description = description;
    }

    public String toString() {
      return taskName;
    }
  }

  /**
   * Pushes a task onto the Task Stack.
   *
   * MOE's UI operates on a stack model. Tasks get pushed onto the stack and then what is popped
   * must be the top task on the stack, allowing nesting only.
   *
   * @param task  the name of the task; should be sensical to a computer
   * @param description  a description of what MOE is about to do, suitable for a user
   *
   * @returns the Task created
   */
  public Task pushTask(String task, String description) {
    Task t = new Task(task, description);
    stack.addFirst(t);
    return t;
  }

  /**
   * Pops a task from the Task Stack.
   *
   * @param task  the task to pop. This must be the task on the top of the stack.
   * @param result  the result of the task, if applicable, or "".
   *
   * @throws MoeProblem  if task is not on the top of the stack
   */
  public void popTask(Task task, String result) {
    if (stack.isEmpty()) {
      throw new MoeProblem(
          String.format("Tried to end task %s, but stack is empty", task.taskName));
    }

    Task top = stack.removeFirst();

    if (top != task) {
      throw new MoeProblem(
          String.format("Tried to end task %s, but stack contains: %s", task.taskName,
                        stack.toString()));
    }
  }

  // TODO(dbentley): there should be errorTask, which reports that the task was finished in error.

}
