// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.gson.annotations.SerializedName;

/**
 * Configuration for a MOE Step.
 *
 * @author dbentley@google.com (Dan Bentley)
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
