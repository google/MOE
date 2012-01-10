// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Configuration for a MOE Translator
 *
 * @author dbentley@google.com (Daniel Bentley)
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

  void validate() throws InvalidProject {
    InvalidProject.assertNotEmpty(
        fromProjectSpace, "Translator requires from_project_space");
    InvalidProject.assertNotEmpty(
        toProjectSpace, "Translator requires to_project_space");
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
