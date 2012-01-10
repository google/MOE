// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.tasks;

import com.google.devtools.moe.client.MoeOptions;
import com.google.inject.Provides;

/**
 * A Task is a unit of MOE work. That is, a Task can be run directly by a user.
 * This allows a user to get an error from a 4-hour run of MOE and run the 30 seconds that
 * actually failed.
 *
 * For instance, say FooTask needs to call BarTask as part of Foo'ing. Then FooTask will have
 * a constructor:
 *
 * FooTask(TaskExecutor executor, BarTaskCreator barCreator)
 *
 * Then FooTask.execute will:
 *   BarTask b = barCreator.create(Param p);
 *   ResultType result = b.execute();
 *
 * The Task will describe to the user "now running Moe bar --flagname=flagvalue"
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public abstract class Task<Result> {

  /**
   * Performs the work of the Task.
   *
   * This should only be called by Task.execute().
   */
  abstract protected Result executeImplementation();

  /**
   * Performs the work of the Task and describes what is happening.
   *
   * Should be called by anyone.
   */
  public Result execute() {
    // TODO(dbentley): integrate with the UI.
    return executeImplementation();
  }

  /**
   * Explains what this Task is doing/has done. Only useful when running at the top-level.
   */
  public static class Explanation {
    public final String message;
    public final int exitCode;
    public Explanation(String message, int exitCode) {
      this.message = message;
      this.exitCode = exitCode;
    }
  }

  public Explanation executeAtTopLevel() {
    return explain(execute());
  }

  /**
   * Explains what this Task is doing/has done.
   */
  abstract protected Explanation explain(Result result);

  public static interface TaskCreator<T> {
    @Provides
    public Task<T> createTaskFromCommandLine(MoeOptions args);
  }
}
