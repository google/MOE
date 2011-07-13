// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Configuration for a MOE Repository.
 *
 * @author nicksantos@google.com (Nick Santos)
 */
public class RepositoryConfig {
  // Initializing this to nil allows it to be used in switch
  // statements without any NPEs.
  private RepositoryType type = RepositoryType.nil;

  private String url;
  @SerializedName("project_space")
  private String projectSpace;

  @SerializedName("build_target")
  private String buildTarget;

  @SerializedName("package")
  private String buildTargetPackage;


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
    List<String> result = paths == null ? Lists.<String>newArrayList() :
        Lists.newArrayList(paths);
    return result;
  }

  public String getBuildTarget() {
    String result = buildTarget;
    return result;
  }

}
