// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.devtools.moe.client.repositories.Revision;

/**
 * An Equivalence holds two Revisions which represent the same files as they appear
 * in different respositories
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

  /**
   * @param revision  the other Revision in this Equivalence
   *
   * @return  the Revision not revision in this Equivalence, or null if this Equivalence
   *          does not contain revision as one of its Revisions
   */
  public Revision getOtherRevision(Revision revision) {
    if (hasRevision(revision)) {
      if (rev1.equals(revision)) {
        return rev2;
      } else {
        return rev1;
      }
    }
    return null;
  }

  @Override
  public int hashCode() {
    return rev1.hashCode() + rev2.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Equivalence) {
      Equivalence equivalenceObj = (Equivalence) obj;
      return (equivalenceObj.hasRevision(rev1) &&
              equivalenceObj.hasRevision(rev2));
    }
    return false;
  }
}
