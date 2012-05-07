// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.devtools.moe.client.repositories.Revision;

import java.util.Set;

/**
 * An abstraction of MOE's database.
 *
 */
public interface Db {

  /**
   * Adds an Equivalence to this Db.
   */
  public void noteEquivalence(Equivalence equivalence);

  /**
   * Returns the Revisions in Repository {@code otherRepository} that are equivalent to the given
   * Revision.
   *
   * @param revision  the Revision to find equivalent revisions for
   * @param otherRepository  the Repository to find equivalent revisions in
   */
  public Set<Revision> findEquivalences(Revision revision, String otherRepository);

  /**
   * Stores a SubmittedMigration in this Db. Migrations are stored along with Equivalences to give
   * full historical information for runs of MOE, as not all migrations result in an Equivalence.
   *
   * @param migration  the SubmittedMigration to add to the database
   * @return true if the SubmittedMigration was newly added, false if it was already in this Db
   */
  public boolean noteMigration(SubmittedMigration migration);

  /**
   * Writes the Db contents as plain text to the given path.
   */
  public void writeToLocation(String dbLocation);
}
