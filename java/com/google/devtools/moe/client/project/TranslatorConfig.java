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

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Configuration for a MOE Translator
 */
public class TranslatorConfig {

  @SerializedName("from_project_space")
  private String fromProjectSpace;

  @SerializedName("to_project_space")
  private String toProjectSpace;

  private List<StepConfig> steps;

  @SerializedName("inverse")
  private boolean isInverse;

  public TranslatorConfig() {} // Constructed by gson

  public String getFromProjectSpace() {
    return fromProjectSpace;
  }

  public String getToProjectSpace() {
    return toProjectSpace;
  }

  public List<StepConfig> getSteps() {
    return steps;
  }

  public boolean isInverse() {
    return isInverse;
  }

  public ScrubberConfig scrubber() {
    if (getSteps() != null) {
      for (StepConfig step : getSteps()) {
        if (step.getEditorConfig().getType() == EditorType.scrubber) {
          return step.getEditorConfig().getScrubberConfig();
        }
      }
    }
    return null;
  }

  void validate() throws InvalidProject {
    InvalidProject.assertNotEmpty(fromProjectSpace, "Translator requires from_project_space");
    InvalidProject.assertNotEmpty(toProjectSpace, "Translator requires to_project_space");
    if (isInverse) {
      InvalidProject.assertTrue(steps == null, "Inverse translator can't have steps");
    } else {
      InvalidProject.assertTrue(steps != null, "Translator requires steps");
      for (StepConfig s : steps) {
        s.validate();
      }
    }
  }
}
