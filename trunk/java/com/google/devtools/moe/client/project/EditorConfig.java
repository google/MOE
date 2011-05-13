// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonObject;

/**
 * Configuration for a MOE Editor.
 *
 * @author dbentley@google.com (Dan Bentley)
 */
public class EditorConfig {
  private EditorType type;
  @SerializedName("scrubber_config")
  private JsonObject scrubberConfig;

  private EditorConfig() {} // Constructed by gson

  public EditorType getType() {
    return type;
  }

  public JsonObject getScrubberConfig() {
    return scrubberConfig;
  }
}
