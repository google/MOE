// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.migrations;

import com.google.devtools.moe.client.repositories.MetadataScrubberConfig;

/**
 * A Migration represents the series of changes needed to make the from_repository equivalent to
 * the to_repository. This object holds the data necessary to make such a migration. As of now,
 * this is just a wrapper around the data in a MigrationConfig.
 *
 */
public class Migration {

  public final String name;
  public final String fromRepository;
  public final String toRepository;
  public final boolean separateRevisions;
  public final MetadataScrubberConfig metadataScrubberConfig;

  public Migration(MigrationConfig config) {
    this.name = config.getName();
    this.fromRepository = config.getFromRepository();
    this.toRepository = config.getToRepository();
    this.separateRevisions = config.getSeparateRevisions();
    this.metadataScrubberConfig = config.getMetadataScrubberConfig();
  }
}
