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
