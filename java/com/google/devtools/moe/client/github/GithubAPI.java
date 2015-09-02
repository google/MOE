// Copyright 2015 The MOE Authors All Rights Reserved.
package com.google.devtools.moe.client.github;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.AutoValueGsonAdapter;
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

    abstract String html_url();

    public String htmlUrl() {
      return html_url();
    }

    public abstract String title();

    public abstract Repo repo();

    public abstract Commit head();

    public abstract Commit base();

    public abstract IssueState state();

    public abstract boolean merged();

    abstract MergeableState mergeable_state();

    public MergeableState mergeableState() {
      return mergeable_state();
    }
  }

  /** Represents the metadata from a commit on github.com. */
  @AutoValue
  @JsonAdapter(AutoValueGsonAdapter.class)
  public abstract static class Commit {
    public abstract long id();

    public abstract User user();

    public abstract Repo repo();

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

    abstract String clone_url(); // gson-naming

    public String cloneUrl() {
      return clone_url();
    }
  }

  /** Represents the metadata for a user on github.com. */
  @AutoValue
  @JsonAdapter(AutoValueGsonAdapter.class)
  public abstract static class User {
    public abstract long id();

    public abstract String login();
  }

  /**
   * The current status of the pull request issue (open, closd)
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

