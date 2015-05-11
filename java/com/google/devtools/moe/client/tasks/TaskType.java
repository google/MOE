// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.tasks;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.MoeOptions;

import dagger.Module;
import dagger.Provides;

import java.util.Map;

/**
 * The module for creating a task from the command line.
 *
 */
@Module
public class TaskType {

  private final String desc;
  private final MoeOptions options;
  private final Class<? extends Task.TaskCreator> taskCreatorClass;

  public MoeOptions getOptions() {
    return options;
  }

  @Provides
  public MoeOptions provideOptions() {
    return options;
  }

  @Provides Task.TaskCreator provideTaskCreator() {
    try {
      return taskCreatorClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException("TaskCreator could not be created.", e);
    }
  }

  @Provides
  public Task provideTask(Task.TaskCreator tc, MoeOptions options) {
    return tc.createTaskFromCommandLine(options);
  }

  public TaskType(String desc, MoeOptions options,
                  Class<? extends Task.TaskCreator> taskCreatorClass) {
    this.desc = desc;
    this.options = options;
    this.taskCreatorClass = taskCreatorClass;
  }

  public static final Map<String, TaskType> TASK_MAP = ImmutableMap.of(
      HelloTask.commandLineName, new TaskType(
          "Prints welcome message",
          new HelloTask.HelloOptions(),
          HelloTask.HelloTaskCreator.class));
}
