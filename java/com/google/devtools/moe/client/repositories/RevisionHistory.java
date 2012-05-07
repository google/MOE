// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import javax.annotation.Nullable;

/**
 * An abstraction of a Repository's Revision History.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface RevisionHistory {

  /**
   * Finds the highest revision less than or equal to revId.
   *
   * @param revId  the id of the revision to start at. Null for head.
   * @return the highest Revision less than or equal to revId.
   */
  public Revision findHighestRevision(@Nullable String revId);

  /**
   * Reads the metadata for a given revision in the same repository.
   *
   * @param revision  the revision to parse metadata for.
   * @return a RevisionMetadata containing metadata for revision
   */
  public RevisionMetadata getMetadata(Revision revision);

  /**
   * Starting at the specified revision, searches the revision history backwards, stopping at
   * matching Revisions. {@link RevisionMatcher#matches(Revision)} is called on the given
   * RevisionMatcher for the starting Revisions, then their parents, then the parents' parents, and
   * so on. Then, {@link RevisionMatcher#makeResult(RevisionGraph, java.util.List)} is called with
   * the non-matching and matching Revisions found, and the result is returned.
   *
   * <p>For example, say you have a RevisionMatcher that matches on revision IDs starting with
   * "match", and this method is called on this Revision history, starting at the top:
   * <pre>
   *       nonmatch_4
   *       /         \
   * nonmatch_3a  nonmatch_3b
   *       \         /    \
   *       nonmatch_2a  match_2b
   *           \           /   \
   *              match_1      ...
   *                 |
   *                ...
   * </pre>
   *
   * <p>History is traversed in breadth-first order, and {@link RevisionMatcher#matches(Revision)}
   * is called on each Revision. The traversal doesn't proceed past any matching Revision. Finally,
   * {@link RevisionMatcher#makeResult(RevisionGraph, java.util.List)} is called with a
   * RevisionGraph of the non-matching Revisions (in this case, nonmatch_4, nonmatch_3a,
   * nonmatch_3b, and nonmatch_2a in that breadth-first order) and a List of the matching Revisions
   * in the order encountered (in this case, match_2b and match_1). This method returns the
   * (arbitrary) result of that call.
   *
   * @param revision  the Revision to start at. If null, then start at head revision(s).
   * @param matcher  the RevisionMatcher to apply
   * @return the result of the search, as specified by the type of RevisionMatcher
   */
  //TODO(user): allow specifying multiple Revisions (for case of multiple heads)
  public <T> T findRevisions(@Nullable Revision revision, RevisionMatcher<T> matcher);
}
