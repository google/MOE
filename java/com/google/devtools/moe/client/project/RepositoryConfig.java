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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Configuration for a MOE Repository.
 *
 * @author nicksantos@google.com (Nick Santos)
 */
public class RepositoryConfig {
  private String type;

  private String url;

  @SerializedName("project_space")
  private String projectSpace = "public";

  @SerializedName("build_target")
  private String buildTarget;

  @SerializedName("package")
  private String buildTargetPackage;

  @SerializedName("preserve_authors")
  private boolean preserveAuthors;


  private List<String> paths;

  /**
   * List of filepath regexes to ignore in this repo, e.g. files specific to this repo that are
   * not to be considered in translation b/w codebases.
   */
  @SerializedName("ignore_file_patterns")
  private List<String> ignoreFilePatterns = ImmutableList.of();

  /**
   * Legacy name for {@code ignore_file_patterns} which was always regular expression patterns.
   */
  @Deprecated
  @SerializedName("ignore_file_res")
  private List<String> ignoreFileRes = ImmutableList.of();

  @SerializedName("executable_file_patterns")
  private List<String> executableFilePatterns = ImmutableList.of();

  /**
   * Legacy name for {@code executable_file_patterns} which was always regular expression patterns.
   */
  @Deprecated
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

  public String getType() {
    return type;
  }

  public String getProjectSpace() {
    return projectSpace;
  }

  public List<String> getPaths() {
    List<String> result = paths == null ? Lists.<String>newArrayList() : Lists.newArrayList(paths);
    return result;
  }

  public String getBuildTarget() {
    String result = buildTarget;
    return result;
  }

  public boolean getPreserveAuthors() {
    return preserveAuthors;
  }


  /**
   * Returns a list of regular expression patterns whose matching files will be ignored when
   * calculating the relevant file set to compare codebases.
   */
  public List<String> getIgnoreFilePatterns() {
    if (!ignoreFilePatterns.isEmpty()) {
      if (!ignoreFileRes.isEmpty()) {
        throw new InvalidProject(
            "ignore_file_res is a deprecated configuration field.  it is replaced with "
                + "ignore_file_patterns. Only one of these should be set.");
      }
      return ignoreFilePatterns;
    }
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
   * Returns a list of pattern strings for file paths that should be marked executable. For version
   * control or build systems that don't support the executable bit, use these patterns to indicate
   * which files should be marked executable. Any files that don't match one of these patterns will
   * be marked non-executable.
   */
  public List<String> getExecutableFilePatterns() {
    if (!executableFilePatterns.isEmpty()) {
      if (!executableFileRes.isEmpty()) {
        throw new InvalidProject(
            "executable_file_res is a deprecated configuration field.  it is replaced with "
                + "executable_file_patterns. Only one of these should be set.");
      }
      return executableFilePatterns;
    }
    return executableFileRes;
  }

  /**
   * Validates that the supplied
   * {@link com.google.devtools.moe.client.repositories.RepositoryType.Factory} targets the
   * correct repo type, throwing an {@link InvalidProject} exception if it does not.
   */
  public void checkType(RepositoryType.Factory repositoryFactory) throws InvalidProject {
    if (!repositoryFactory.type().equals(getType())) {
      // TODO(cgruber): Make it so this can't happen at runtime, ever, and throw AssertionError.
      throw new InvalidProject(
          "Invalid repository type '%s' for %s",
          getType(),
          repositoryFactory.getClass().getSimpleName());
    }
  }

  /**
   * Modified copy creator, that supplies a clone with the branch field altered.
   */
  public RepositoryConfig copyWithBranch(String branch) {
    RepositoryConfig newConfig = Utils.cloneGsonObject(this);
    newConfig.branch = branch;
    return newConfig;
  }

  /**
   * Modified copy creator, that supplies a clone with the branch field altered.
   */
  public RepositoryConfig copyWithUrl(String url) {
    RepositoryConfig newConfig = Utils.cloneGsonObject(this);
    newConfig.url = url;
    return newConfig;
  }

  @SuppressWarnings("unused")
  void validate() throws InvalidProject {}
}
