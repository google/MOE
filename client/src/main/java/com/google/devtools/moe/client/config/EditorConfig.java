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

package com.google.devtools.moe.client.config;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.InvalidProject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

/**
 * Configuration for a MOE Editor.
 */
@AutoValue
public abstract class EditorConfig {
  @Nullable
  public abstract EditorType type();

  @Nullable
  @SerializedName("scrubber_config") // TODO(cushon): remove pending rharter/auto-value-gson#18
  public abstract ScrubberConfig scrubberConfig();

  @Nullable
  @SerializedName("command_string") // TODO(cushon): remove pending rharter/auto-value-gson#18
  public abstract String commandString();

  @Nullable
  public abstract JsonObject mappings();

  @SerializedName("use_regex") // TODO(cushon): remove pending rharter/auto-value-gson#18
  public abstract boolean useRegex();

  // TODO(cgruber): Push validation around the whole structure.
  public void validate() throws InvalidProject {
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

  public static TypeAdapter<EditorConfig> typeAdapter(Gson gson) {
    return new AutoValue_EditorConfig.GsonTypeAdapter(gson);
  }
}
