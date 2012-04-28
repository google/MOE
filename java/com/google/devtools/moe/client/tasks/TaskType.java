// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.tasks;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.MoeOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.util.Map;

/**
 * The Guice module for creating a task from the command line.
 *
 */
public class TaskType extends AbstractModule {

  private final String desc;
  private final MoeOptions options;
  private final Class<? extends Task.TaskCreator> taskCreatorClass;

  public MoeOptions getOptions() {
    return options;
  }

  protected void configure() {
    bind(MoeOptions.class).toInstance(options);
    bind(Task.TaskCreator.class).to(taskCreatorClass);
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
