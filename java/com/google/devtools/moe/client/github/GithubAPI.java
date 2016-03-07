/*
 * Copyright (c) 2015 Google, Inc.
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
package com.google.devtools.moe.client.github;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.gson.AutoValueGsonAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * Gson-ready value types representing the needed subset of Github's restful API.
 */
public final class GithubAPI {

  /** Represents the metadata from a pull-request on github.com. */
  @AutoValue
  @JsonAdapter(AutoValueGsonAdapter.class)
  public abstract static class PullRequest {
    public abstract long number();

    public abstract String url();

    public abstract String htmlUrl();

    public abstract String title();

    public abstract Repo repo();

    public abstract Commit head();

    public abstract Commit base();

    public abstract IssueState state();

    public abstract boolean merged();

    public abstract MergeableState mergeableState();

  }

  /** Represents the metadata from a commit on github.com. */
  @AutoValue
  @JsonAdapter(AutoValueGsonAdapter.class)
  public abstract static class Commit {
    public abstract long id();

    public abstract User user();

    public abstract Repo repo();

    /**
     * In a pull request, this is the owner/branch information that identifies the branch for
     * which this commit is a HEAD pointer.
     */
    public abstract String ref();

    public abstract String sha();
  }

  /** Represents the metadata for a repository as hosted on github.com. */
  @AutoValue
  @JsonAdapter(AutoValueGsonAdapter.class)
  public abstract static class Repo {
    public abstract long id();

    public abstract String name();

    public abstract User owner();

    public abstract String cloneUrl();
  }

  /** Represents the metadata for a user on github.com. */
  @AutoValue
  @JsonAdapter(AutoValueGsonAdapter.class)
  public abstract static class User {
    public abstract long id();

    public abstract String login();
  }

  /**
   * The current status of the pull request issue (open, closed)
   */
  public enum IssueState {
    @SerializedName("open")
    OPEN,
    @SerializedName("closed")
    CLOSED;
  }

  /**
   * The current state of the pull-request with respect to the safety of merging cleanly against
   * the base commit.
   */
  public enum MergeableState {
    /** This pull request could be cleanly merged against its base */
    @SerializedName("clean")
    CLEAN,
    /** Clean for merging but invalid (failing build, or some other readiness check */
    @SerializedName("unstable")
    UNSTABLE,
    /** Github hasn't computed mergeability, but this request has kicked off a job.  Retry. */
    @SerializedName("unknown")
    UNKNOWN,
  }

  private GithubAPI() {}
}

