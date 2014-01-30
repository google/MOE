// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.devtools.moe.client.FileSystem.Lifetime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.inject.Inject;

/**
 * User Interface interface for MOE.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public abstract class Ui {

  @Inject protected FileSystem fileSystem;

  /**
   * The name of the Task pushed to the Ui for clean-up when MOE is about to exit. This name is
   * used, for example, to set a temp dir's lifetime to "clean up when MOE is about to exit".
   */
  public static final String MOE_TERMINATION_TASK_NAME = "moe_termination";

  protected final Deque<Task> stack = new ArrayDeque<Task>();

  /**
   * Sends an informational message to the user.
   *
   * @param msg  the informational message.
   */
  public abstract void info(String msg);

  /**
   * Reports an error to the user.
   */
  public abstract void error(String msg);

  /**
   * Reports an error to the user.
   */
  public abstract void error(Throwable e, String msg);

  /** Sends a debug message to the logs. */
  public abstract void debug(String msg);

  public static class Task {
    public final String taskName;
    public final String description;

    public Task(String taskName, String description) {
      this.taskName = taskName;
      this.description = description;
    }

    @Override
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
   * Pops a task from the Task Stack. No files or directories are persisted beyond this Task. After
   * the Task is popped, temp dirs are cleaned up via {@link FileSystem#cleanUpTempDirs()}.
   *
   * @param task  the task to pop. This must be the task on the top of the stack.
   * @param result  the result of the task, if applicable, or "".
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

    if (fileSystem != null) {
      try {
        fileSystem.cleanUpTempDirs();
      } catch (IOException ioEx) {
        error(ioEx, "Error cleaning up temp dirs");
        throw new MoeProblem("Error cleaning up temp dirs: " + ioEx);
      }
    }
  }

  /**
   * Pops a task from the Task Stack, persisting the given File beyond this Task. In general, use
   * this if you call {@link FileSystem#getTemporaryDirectory(String, Lifetime)} within a Task and
   * need to keep the resulting temp dir past completion of this Task.
   *
   * If there is a parent Task on the stack after the one being popped here, then
   * {@code persistentResult} will be cleaned up when the parent Task is popped (unless it's
   * persisted within that Task too). If there is no parent Task, then {@code persistentResult}
   * will be persisted beyond MOE execution. So any results persisted beyond a top-level Task
   * constitute outputs of MOE execution.
   */
  public void popTaskAndPersist(Task task, File persistentResult) {
    if (fileSystem != null) {
      Lifetime newLifetime;
      if (stack.size() == 1) {
        newLifetime = Lifetimes.persistent();
      } else {
        Task parentTask = Iterables.get(stack, 1);
        newLifetime = new TaskLifetime(parentTask);
      }
      fileSystem.setLifetime(persistentResult, newLifetime);
    }

    popTask(task, persistentResult.getAbsolutePath());
  }

  Lifetime currentTaskLifetime() {
    Preconditions.checkState(!stack.isEmpty());
    return new TaskLifetime(stack.peek());
  }

  Lifetime moeExecutionLifetime() {
    return new MoeExecutionLifetime();
  }

  /**
   * A {@code Lifetime} for a temp dir that should be cleaned up when the given Task is completed.
   */
  private class TaskLifetime implements Lifetime {

    private final Task task;

    TaskLifetime(Task task) {
      this.task = task;
    }

    @Override public boolean shouldCleanUp() {
      return !stack.contains(task);
    }
  }

  /**
   * A {@code Lifetime} for a temp dir that should be cleaned up when MOE completes execution.
   */
  private class MoeExecutionLifetime implements Lifetime {

    @Override public boolean shouldCleanUp() {
      return !stack.isEmpty() && stack.peek().taskName.equals(MOE_TERMINATION_TASK_NAME);
    }
  }

  // TODO(dbentley): there should be errorTask, which reports that the task was finished in error.

}
