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
package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Configuration for a MOE metadata scrubber.
 */
public class MetadataScrubberConfig {

  private List<String> usernamesToScrub;
  private boolean scrubConfidentialWords = true;
  private List<String> sensitiveRes = ImmutableList.of();

  /**
   * Formatting for changelog adapted from fromRepository for commits in toRepository. See
   * {@link DescriptionMetadataScrubber}.
   */
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

  /** A list of regular expressions for sensitive text to be scrubbed from revision metadata. */
  public List<String> getSensitiveRes() {
    return sensitiveRes;
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

    if (scrubConfidentialWords) {
      // Here is where you can use a MetadataScrubber that scrubs confidential words.
    }

    scrubbersBuilder.add(new PublicSectionMetadataScrubber());

    scrubbersBuilder.add(new DescriptionMetadataScrubber(logFormat));

    return scrubbersBuilder.build();
  }
}
