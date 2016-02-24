package com.google.devtools.moe.client;

/**
 * Class describing the task data structure.
 */
public class Task {
  
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
  } // TODO(cgruber) make this lazy once Task is an autovalue.

  @Override
  public String toString() {
    return taskName;
  }
  
}
