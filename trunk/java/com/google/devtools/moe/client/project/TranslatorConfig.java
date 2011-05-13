// Copyright 2011 Google Inc. All Rights Reserved.

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
}
