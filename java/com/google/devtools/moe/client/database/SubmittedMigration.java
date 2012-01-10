// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.common.base.Objects;
import com.google.devtools.moe.client.repositories.Revision;

/**
 * A SubmittedMigration holds information about a completed migration.
 *
 * It differs from an Equivalence in that a SubmittedMigration has a direction associated with its
 * Revisions.
 *
 */
public class SubmittedMigration {

  public final Revision fromRevision;
  public final Revision toRevision;

  /**
   * @param fromRevision  the Revision in the source repository
   * @param toRevision  the Revision in the destination repository
   */
  public SubmittedMigration(Revision fromRevision, Revision toRevision) {
    this.fromRevision = fromRevision;
    this.toRevision = toRevision;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fromRevision, toRevision);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SubmittedMigration) {
      SubmittedMigration migrationObj = (SubmittedMigration) obj;
      return (migrationObj.fromRevision.equals(fromRevision) &&
              migrationObj.toRevision.equals(toRevision));
    }
    return false;
  }

  @Override
  public String toString() {
    return fromRevision.toString() + " ==> " + toRevision.toString();
  }
}
