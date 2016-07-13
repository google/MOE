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

package com.google.devtools.moe.client.project;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.gson.AutoValueGsonAdapter;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;

/**
 * Configuration for a MOE Editor.
 */
@AutoValue
@JsonAdapter(AutoValueGsonAdapter.class)
public abstract class EditorConfig {
  public abstract EditorType type();

  public abstract ScrubberConfig scrubberConfig();

  public abstract String commandString();

  public abstract JsonObject mappings();

  public abstract boolean useRegex();

  void validate() throws InvalidProject {
    InvalidProject.assertNotNull(type(), "Missing type in editor");
  }

  public static EditorConfig create(
      EditorType type,
      ScrubberConfig scrubberConfig,
      String commandString,
      JsonObject mappings,
      boolean useRegex) {
    return new AutoValue_EditorConfig(type, scrubberConfig, commandString, mappings, useRegex);
  }
}
