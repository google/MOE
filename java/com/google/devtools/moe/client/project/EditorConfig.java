// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * Configuration for a MOE Editor.
 *
 * @author dbentley@google.com (Dan Bentley)
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
