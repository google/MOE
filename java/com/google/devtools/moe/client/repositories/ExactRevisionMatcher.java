// Copyright 2015 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher;

import java.util.List;

/**
 * A RevisionMatcher is used to crawl a repository's revision history, stopping at matching
 * Revisions, and returning an arbitrary result depending on the non-matching and matching
 * Revisions found.
 */
public class ExactRevisionMatcher implements RevisionMatcher<ExactRevisionMatcher.Result> {
  private final Revision branchPointRevision;

  public ExactRevisionMatcher(Revision branchPointRevision) {
    this.branchPointRevision = branchPointRevision;
  }

  /**
   * Returns whether this Revision matches. If it doesn't, then history-crawling should continue
   * through its parents.
   */
  @Override
  public boolean matches(Revision revision) {
    return revision.equals(branchPointRevision);
  }

  /**
   * Returns a result of crawling a repository's revision history, along a branch, back to a
   * specific branch point.
   */
  @Override
  public ExactRevisionMatcher.Result
      makeResult(RevisionGraph nonMatching, List<Revision> matching) {
    switch (matching.size()) {
      case 0:
        throw new MoeProblem(
            "No matching revisions in history. The branch may have no commits or be mislabeled.");
      case 1:
        return Result.create(nonMatching);
      default:
        throw new MoeProblem("Revision history matched %s branch points", matching.size());
    }
  }

  /**
   * The result of crawling a revision history with a {@link RepositoryEquivalenceMatcher}.
   * Stores the revisions found since any equivalence, and the equivalences themselves.
   */
  @AutoValue
  public abstract static class Result {
    /**
     * Returns a {@link RevisionGraph} of Revisions since equivalence, or since the start of repo
     * history if no equivalence was found.
     */
    public abstract RevisionGraph revisions();

    static Result create(RevisionGraph revisions) {
      return new AutoValue_ExactRevisionMatcher_Result(revisions);
    }
  }
}
