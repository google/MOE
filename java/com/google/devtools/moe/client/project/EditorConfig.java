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

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * Configuration for a MOE Editor.
 */
public class EditorConfig {

  private EditorType type;

  //only used for scrubbing editors
  @SerializedName("scrubber_config")
  private ScrubberConfig scrubberConfig;

  //only used for shell editors
  @SerializedName("command_string")
  private String commandString;

  //only used for renaming editors
  private JsonObject mappings;

  @SerializedName("use_regex")
  private boolean useRegex = false;

  private EditorConfig() {} // Constructed by gson

  public EditorType getType() {
    return type;
  }

  public ScrubberConfig getScrubberConfig() {
    return scrubberConfig;
  }

  public String getCommandString() {
    return commandString;
  }

  public JsonObject getMappings() {
    return mappings;
  }

  public boolean getUseRegex() {
    return useRegex;
  }

  void validate() throws InvalidProject {
    InvalidProject.assertNotNull(type, "Missing type in editor");
  }
}
