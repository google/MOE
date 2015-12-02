package com.google.devtools.moe.client.project;

import java.util.List;

/**
 * Configuration for the scrubber usernames.
 */
public class UsernamesConfig {

  private List<String> scrubbableUsernames;
  private List<String> publishableUsernames;

  private UsernamesConfig() {}

  public List<String> getScrubbableUsernames() {
    return scrubbableUsernames;
  }

  public List<String> getPublishableUsernames() {
    return publishableUsernames;
  }
}
