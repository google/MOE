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

import com.google.devtools.moe.client.InvalidProject;
import com.google.gson.annotations.SerializedName;

/**
 * Configuration for a MOE Step.
 */
public class StepConfig {

  private String name;

  @SerializedName("editor")
  private EditorConfig editorConfig;

  private StepConfig() {} // Constructed by gson

  public String getName() {
    return name;
  }

  public EditorConfig getEditorConfig() {
    return editorConfig;
  }

  void validate() throws InvalidProject {
    InvalidProject.assertNotEmpty(name, "Missing name in step");
    InvalidProject.assertNotNull(editorConfig, "Missing editor in step");
    editorConfig.validate();
  }
}
