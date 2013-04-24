// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
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
  private String projectSpace = "public";

  @SerializedName("build_target")
  private String buildTarget;

  @SerializedName("package")
  private String buildTargetPackage;


  private List<String> paths;

  /**
   * List of filepath regexes to ignore in this repo, e.g. files specific to this repo that are
   * not to be considered in translation b/w codebases.
   */
  @SerializedName("ignore_file_res")
  private List<String> ignoreFileRes = ImmutableList.of();

  @SerializedName("executable_file_res")
  private List<String> executableFileRes = ImmutableList.of();

  /**
   * List of regexes for filepaths to ignore changes to. In other words, in the repo's Writer,
   * changes to filepaths matching any of these regexes will be ignored -- no additions, deletions,
   * or modifications of matching filepaths.
   */
  @SerializedName("ignore_incoming_changes_res")
  private List<String> ignoreIncomingChangesRes = ImmutableList.of();

  @SerializedName("branch")
  private String branch = null;

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


  public List<String> getIgnoreFileRes() {
    return ignoreFileRes;
  }

  public List<String> getIgnoreIncomingChangesRes() {
    return ignoreIncomingChangesRes;
  }

  /**
   * Returns the branch of this repository to use. New changes to migrate will be searched for only
   * on this branch, and incoming changes will be written onto this branch.
   */
  public Optional<String> getBranch() {
    return Optional.fromNullable(branch);
  }

  /**
   * Returns a list of regexes for file paths that should be marked executable. For version control
   * or build systems that don't support the executable bit, use these regexes to indicate which
   * files should be marked executable. Any files that don't match one of these regexes will be
   * marked non-executable.
   */
  public List<String> getExecutableFileRes() {
    return executableFileRes;
  }

  void validate() throws InvalidProject {
  }
}
