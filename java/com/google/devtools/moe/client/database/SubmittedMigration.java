// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.AutoValueGsonAdapter;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.gson.annotations.JsonAdapter;

/**
 * A SubmittedMigration holds information about a completed migration.
 *
 * It differs from an Equivalence in that a SubmittedMigration has a direction associated with its
 * Revisions.
 *
 */
@AutoValue
@JsonAdapter(AutoValueGsonAdapter.class)
public abstract class SubmittedMigration {
  public static SubmittedMigration create(Revision fromRevision, Revision toRevision) {
    return new AutoValue_SubmittedMigration(fromRevision, toRevision);
  }

  /** The {@link Revision} that represents the source of this migrated commit */
  public abstract Revision fromRevision();

  /** The {@link Revision} that represents the destination of this migrated commit */
  public abstract Revision toRevision();

  @Override
  public String toString() {
    return fromRevision() + " ==> " + toRevision();
  }
}
