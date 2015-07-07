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
   * non-matching Revisions (i.e. those where {@link #matches(Revision)} returned {@code false}),
   * and second, the matching Revisions in the order they were encountered.
   *
   * <p>For example, {@link com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher}
   * returns a result encapsulating the non-matching Revisions (those since equivalence) and the
   * Equivalences corresponding to matching Revisions.
   *
   * @see RevisionHistory#findRevisions(Revision, RevisionMatcher,
   * com.google.devtools.moe.client.repositories.RevisionHistory.SearchType)
   */
  T makeResult(RevisionGraph nonMatching, List<Revision> matching);
}
