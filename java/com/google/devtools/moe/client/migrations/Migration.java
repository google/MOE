// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.migrations;

import com.google.common.base.Joiner;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;

import java.util.List;

/**
 * A Migration represents a change to be ported from one {@link Repository} to another.
 *
 */
// TODO(cgruber) @AutoValue
public class Migration {

  /** The specification of all Migrations b/w these from and to repos */
  public final MigrationConfig config;
  /** The most recent Equivalence b/w the from and to repos of this Migration */
  public final RepositoryEquivalence sinceEquivalence;
  /** The changes in fromRepository encapsulated by this Migration */
  public final List<Revision> fromRevisions;

  public Migration(
      MigrationConfig config,
      List<Revision> fromRevisions,
      RepositoryEquivalence sinceEquivalence) {
    this.config = config;
    this.sinceEquivalence = sinceEquivalence;
    this.fromRevisions = fromRevisions;
  }

  @Override
  public String toString() {
    return String.format(
        "Migration %s since equivalence (%s) of revisions {%s}",
        config.getName(),
        sinceEquivalence,
        Joiner.on(", ").join(fromRevisions));
  }
}
