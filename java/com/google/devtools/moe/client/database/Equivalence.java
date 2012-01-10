// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.devtools.moe.client.repositories.Revision;

/**
 * An Equivalence holds two Revisions which represent the same files as they appear
 * in different repositories
 *
 * Two Revisions are equivalent when an Equivalence contains both in any order
 *
 */
public class Equivalence {

  private final Revision rev1;
  private final Revision rev2;

  public Equivalence() {
    this.rev1 = new Revision();
    this.rev2 = new Revision();
  } // For gson

  /*
   * Parameters rev1 and rev2 may be passed in either order. The resulting Equivalences are equal.
   */
  public Equivalence(Revision rev1, Revision rev2) {
    this.rev1 = rev1;
    this.rev2 = rev2;
  }

  /**
   * @param revision  the Revision to look for in this Equivalence
   *
   * @return  true if this Equivalence has revision as one of its Revisions
   */
  public boolean hasRevision(Revision revision) {
    return rev1.equals(revision) || rev2.equals(revision);
  }

  /** @return the Revision in this Equivalence for the given repository name */
  public Revision getRevisionForRepository(String repositoryName) {
    if (rev1.repositoryName.equals(repositoryName)) {
      return rev1;
    } else if (rev2.repositoryName.equals(repositoryName)) {
      return rev2;
    } else {
      throw new IllegalArgumentException(
          "Equivalence " + this + " doesn't have revision for " + repositoryName);
    }
  }

  /**
   * @param revision  the other Revision in this Equivalence
   *
   * @return  the Revision not revision in this Equivalence, or null if this Equivalence
   *          does not contain revision as one of its Revisions
   */
  public Revision getOtherRevision(Revision revision) {
    if (hasRevision(revision)) {
      return rev1.equals(revision) ? rev2 : rev1;
    }
    return null;
  }

  /**
   * We override hashCode() so that it is commutative such that two Equivalences with the same
   * Revisions but switched in order will still have the same hash since those two Equivalences are
   * considered equal by the override of equals(...).
   */
  @Override
  public int hashCode() {
    return rev1.hashCode() + rev2.hashCode();
  }

  /**
   * The order of the Revisions does not matter when checking for equality between Equivalences.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Equivalence) {
      Equivalence equivalenceObj = (Equivalence) obj;
      return (equivalenceObj.hasRevision(rev1) &&
              equivalenceObj.hasRevision(rev2));
    }
    return false;
  }

  @Override
  public String toString() {
    return (rev1.toString() + " == " + rev2.toString());
  }
}
