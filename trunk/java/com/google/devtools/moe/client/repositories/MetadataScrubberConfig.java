// Copyright 2011 Google Inc. All Rights Reserved

package com.google.devtools.moe.client.repositories;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Configuration for a MOE metadata scrubber.
 */
public class MetadataScrubberConfig {
  @SerializedName("usernames_to_scrub")
  private List<String> usernamesToScrub;

  @SerializedName("scrub_confidential_words")
  private boolean scrubConfidentialWords = true;

  public MetadataScrubberConfig() {} // Constructed by gson

  public List<String> getUsernamesToScrub() {
    return usernamesToScrub;
  }

  /**
   * Access the boolean field, which can be used as a trigger for
   * the proper MetadataScrubber to scrub any confidential words.
   * @return boolean whether confidential words should be scrubbed
   */
  public boolean getScrubConfidentialWords() {
    return scrubConfidentialWords;
  }
}
