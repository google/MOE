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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration for a MOE Translator
 */
public class TranslatorConfig {

  private String fromProjectSpace;
  private String toProjectSpace;
  private List<StepConfig> steps;
  private boolean inverse;

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
    return inverse;
  }

  public ScrubberConfig scrubber() {
    if (getSteps() != null) {
      for (StepConfig step : getSteps()) {
        if (step.getEditorConfig().type() == EditorType.scrubber) {
          return step.getEditorConfig().scrubberConfig();
        }
      }
    }
    return null;
  }

  public List<ScrubberConfig> scrubbers() {
    if (getSteps() != null) {
      return getSteps().stream()
          .map(StepConfig::getEditorConfig)
          .filter(ec -> ec.type() == EditorType.scrubber)
          .map(EditorConfig::scrubberConfig)
          .collect(Collectors.toList());
    }
    return new ArrayList<>(0);
  }

  public void validate() throws InvalidProject {
    InvalidProject.assertNotEmpty(fromProjectSpace, "Translator requires from_project_space");
    InvalidProject.assertNotEmpty(toProjectSpace, "Translator requires to_project_space");
    if (inverse) {
      InvalidProject.assertTrue(steps == null, "Inverse translator can't have steps");
    } else {
      InvalidProject.assertTrue(steps != null, "Translator requires steps");
      for (StepConfig s : steps) {
        s.validate();
      }
    }
  }
}
