// Copyright 2011 The MOE Authors All Rights Reserved

package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;
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

  /**
   * Formatting for changelog adapted from fromRepository for commits in toRepository. See
   * {@link DescriptionMetadataScrubber}.
   */
  @SerializedName("log_format")
  private String logFormat = "{description}\n\tChange on {date} by {author}";

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

  public String getLogFormat() {
    return logFormat;
  }

  /**
   * @return the list of {@link MetadataScrubber}s as determined by this configuration
   */
  public List<MetadataScrubber> getScrubbers() {
    ImmutableList.Builder<MetadataScrubber> scrubbersBuilder = ImmutableList.builder();

    if (usernamesToScrub != null && !usernamesToScrub.isEmpty()) {
      scrubbersBuilder.add(new MetadataUsernameScrubber(getUsernamesToScrub()));
    }

    if (getScrubConfidentialWords()) {
      // Here is where you can use a MetadataScrubber that scrubs confidential words.
    }

    scrubbersBuilder.add(new PublicSectionMetadataScrubber());

    scrubbersBuilder.add(new DescriptionMetadataScrubber(logFormat));

    return scrubbersBuilder.build();
  }
}
