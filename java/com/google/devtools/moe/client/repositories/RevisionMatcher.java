// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import java.util.List;

/**
 * A RevisionMatcher is used to crawl a repository's revision history, stopping at matching
 * Revisions, and returning an arbitrary result depending on the non-matching and matching
 * Revisions found.
 *
 * @param <T> the type of result returned when all matching Revisions are found and crawling ends
 *
 */
public interface RevisionMatcher<T> {

  /**
   * Returns whether this Revision matches. If it doesn't, then history-crawling should continue
   * through its parents.
   */
  boolean matches(Revision revision);

  /**
   * Returns a result of crawling a repository's revision history, depending on, first, the
   * non-matching Revisions found (i.e. those where {@link #matches(Revision)} returned
   * {@code false}), and second, the matching Revisions in the order they were encountered. Per
   * {@link RevisionHistory#findRevisions(Revision, RevisionMatcher)}, the RevisionGraph of
   * non-matching Revisions is the transitive closure of revision history starting at the given
   * starting Revisions, walking backwards through their parents, and bounded by matching
   * Revisions.
   *
   * For example, {@link com.google.devtools.moe.client.database.EquivalenceMatcher} returns a
   * result encapsulating the non-matching Revisions (those since equivalence) and the Equivalences
   * corresponding to the matching Revisions found in the search.
   *
   * @see RevisionHistory#findRevisions(Revision, RevisionMatcher)
   */
  T makeResult(RevisionGraph nonMatching, List<Revision> matching);
}
