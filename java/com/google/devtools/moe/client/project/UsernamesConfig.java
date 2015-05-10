package com.google.devtools.moe.client.project;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Configuration for the scrubber usernames.
 */
public class UsernamesConfig {

  @SerializedName("scrubbable_usernames")
  private List<String> scrubbableUsernames;

  @SerializedName("publishable_usernames")
  private List<String> publishableUsernames;

  private UsernamesConfig() {
  }

  public List<String> getScrubbableUsernames() {
    return scrubbableUsernames;
  }

  public List<String> getPublishableUsernames() {
    return publishableUsernames;
  }

}
