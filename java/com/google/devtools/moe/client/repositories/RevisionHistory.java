// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.EquivalenceMatcher;

import java.util.Set;

/**
 * An abstraction of a Repository's Revision History.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface RevisionHistory {

  /**
   * Finds the highest revision less than or equal to revId.
   *
   * @param revId  the id of the revision to start at. Empty string for head.
   *
   * @return the highest Revision less than or equal to revId.
   */
  public Revision findHighestRevision(String revId);

  /**
   * Read the metadata for a given revision in the same repository.
   *
   * @param revision  the revision to parse metadata for.
   *
   * @return a RevisionMetadata containing metadata for revision
   */
  public RevisionMetadata getMetadata(Revision revision);

  /**
   * Starting at specified revision, recur until a matching revision is found
   *
   * @param revision  the revision to start at. If null, then start at head revision
   * @param matcher  the matcher to apply
   *
   * @return all non-matching Revisions
   */
  //TODO(user): allow specifying multiple Revisions (for case of multiple heads)
  public Set<Revision> findRevisions(Revision revision, RevisionMatcher matcher);

  /**
   * This is the maximum number of parents to examine when in findLastEquivalence(...). MOE will
   * give up trying to find an equivalence after this limit is reached to prevent the successive
   * calls to getMetadata(...) from causing MOE to hang.
   */
  public static final int MAX_PARENTS_TO_EXAMINE = 400;

  /**
   * Starting at specified revision, recur until an equivalence is found in the matcher's
   * other repository.
   *
   * @param revision  the Revision to start at
   * @param matcher  the EquivalenceMatcher to apply
   *
   * @return the most recent Equivalence
   */
  public Equivalence findLastEquivalence(Revision revision, EquivalenceMatcher matcher);
}
