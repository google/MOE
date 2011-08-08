// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;


import java.util.List;

/**
 * This MetadataScrubber replaces any of the target usernames with the replacement string.
 *
 */
public class MetadataUsernameScrubber extends MetadataScrubber {

  private List<String> usernamesToScrub;
  private final String replacement = "<user>";

  public MetadataUsernameScrubber(List<String> usernames) {
    this.usernamesToScrub = usernames;
  }

  @Override
  public RevisionMetadata scrub(RevisionMetadata rm) {
    return MetadataScrubber.stripFromAllFields(rm, usernamesToScrub, replacement, true);
  }
}
