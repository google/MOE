// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.migrations;

import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.project.InvalidProject;
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

  public MigrationConfig copyWithFromRepository(String alternate) {
    // TODO(cgruber) Rip this mechanism of using string labels
    MigrationConfig clone = Utils.cloneGsonObject(this);
    clone.fromRepository = alternate;
    return clone;
  }

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

  public void validate() throws InvalidProject {
    InvalidProject.assertNotEmpty(name, "Missing name in migration");
    InvalidProject.assertNotEmpty(fromRepository, "Missing from_repository in migration");
    InvalidProject.assertNotEmpty(toRepository, "Missing to_repository in migration");
  }
}
