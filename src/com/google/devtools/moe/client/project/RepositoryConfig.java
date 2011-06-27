// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Configuration for a MOE Repository.
 *
 * @author nicksantos@google.com (Nick Santos)
 */
public class RepositoryConfig {
  private RepositoryType type;
  private String url;
  @SerializedName("project_space")
  private String projectSpace;
  @SerializedName("build_target")
  private String buildTarget;
  // MOE: begin_strip
  @SerializedName("in_components")
  private boolean inComponents;
  // MOE: end_strip
  private List<String> paths;

  private RepositoryConfig() {} // Constructed by gson

  public String getUrl() {
    return url;
  }

  public RepositoryType getType() {
    return type;
  }

  public String getProjectSpace() {
    return projectSpace;
  }

  public List<String> getPaths() {
    return paths;
  }

  public String getBuildTarget() {
    return buildTarget;
  }

  // MOE: begin_strip
  public boolean getInComponents() {
    return inComponents;
  }
  // MOE: end_strip
}
