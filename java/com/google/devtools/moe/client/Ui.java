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

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.devtools.moe.client.FileSystem.Lifetime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

/**
 * Represents the command-line user interface for MOE.
 */
public abstract class Ui implements Messenger {

  //TODO(cgruber): Make this not nullable (No-Op filesystem for testing perhaps?)
  @Nullable
  protected final FileSystem fileSystem;

  protected Ui(@Nullable FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  /**
   * The name of the Task pushed to the Ui for clean-up when MOE is about to exit. This name is
   * used, for example, to set a temp dir's lifetime to "clean up when MOE is about to exit".
   */
  public static final String MOE_TERMINATION_TASK_NAME = "moe_termination";

  protected final Deque<Task> stack = new ArrayDeque<Task>();

  public static class Task {
    public final String taskName;
    public final String description;

    public Task(String taskName, String description) {
      this.taskName = taskName;
      this.description = description;
    }

    public Task(String taskName, String descriptionFormat, Object... args) {
      this.taskName = taskName;
      // TODO(cgruber) make this lazy once Task is an autovalue.
      this.description = String.format(descriptionFormat, args);
    }

    @Override
    public String toString() {
      return taskName;
    }
  }

  /**
   * Pushes a task onto the Task Stack.
   *
   * <p>MOE's UI operates on a stack model. Tasks get pushed onto the stack and then what is popped
   * must be the top task on the stack, allowing nesting only.
   *
   * @param task  the name of the task; should be sensical to a computer
   * @param descriptionFormat  a String.format() template for the description of what MOE is
   *     about to do, suitable for a user.
   * @param formatArgs  arguments which will be used to format the descriptionFormat template
   *
   * @returns the Task created
   */
  public Task pushTask(String task, String descriptionFormat, Object... formatArgs) {
    Task t = new Task(task, descriptionFormat, formatArgs);
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
      throw new MoeProblem("Tried to end task %s, but stack is empty", task.taskName);
    }

    Task top = stack.removeFirst();

    if (top != task) {
      throw new MoeProblem("Tried to end task %s, but stack contains: %s", task.taskName, stack);
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

    @Override
    public boolean shouldCleanUp() {
      return !stack.contains(task);
    }
  }

  /**
   * A {@code Lifetime} for a temp dir that should be cleaned up when MOE completes execution.
   */
  private class MoeExecutionLifetime implements Lifetime {

    @Override
    public boolean shouldCleanUp() {
      return !stack.isEmpty() && stack.peek().taskName.equals(MOE_TERMINATION_TASK_NAME);
    }
  }

  // TODO(dbentley): there should be errorTask, which reports that the task was finished in error.

}
