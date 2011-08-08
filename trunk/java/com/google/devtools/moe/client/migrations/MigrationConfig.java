// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.migrations;

import com.google.devtools.moe.client.repositories.MetadataScrubberConfig;
import com.google.gson.annotations.SerializedName;

/**
 * Configuration for a MOE migration.
 *
 */
public class MigrationConfig {
  private String name;

  @SerializedName("separate_revisions")
  private boolean separateRevisions;

  @SerializedName("from_repository")
  private String fromRepository;

  @SerializedName("to_repository")
  private String toRepository;

  @SerializedName("metadata_scrubber_config")
  private MetadataScrubberConfig metadataScrubberConfig;

  public MigrationConfig() {} // Constructed by gson

  public String getName() {
    return name;
  }

  public boolean getSeparateRevisions() {
    return separateRevisions;
  }

  public String getFromRepository() {
    return fromRepository;
  }

  public String getToRepository() {
    return toRepository;
  }

  public MetadataScrubberConfig getMetadataScrubberConfig() {
    return metadataScrubberConfig;
  }
}
