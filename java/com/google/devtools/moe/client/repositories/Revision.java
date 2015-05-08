// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;

import java.util.List;
import java.util.Objects;

/**
 * A Revision in a source control system.
 *
 * A dumb object with no mutable state.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Revision {

  public final String revId;
  public final String repositoryName;

  public Revision() {
    this.revId = "";
    this.repositoryName = "";
  } // For gson

  public Revision(String revId, String repositoryName) {
    this.revId = revId;
    this.repositoryName = repositoryName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryName, revId);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Revision) {
      Revision revisionObj = (Revision) obj;
      return (Objects.equals(repositoryName, revisionObj.repositoryName) &&
              Objects.equals(revId, revisionObj.revId));
    }
    return false;
  }

  @Override
  public String toString() {
    return this.repositoryName + "{" + revId + "}";
  }

  /**
   * Return the list of Revisions given by a RepositoryExpression like "internal(revision=3,4,5)".
   */
  public static List<Revision> fromRepositoryExpression(
      RepositoryExpression repoEx, ProjectContext context) {
    Repository repo = context.getRepository(repoEx.getRepositoryName());
    if (Strings.isNullOrEmpty(repoEx.getOption("revision"))) {
      throw new MoeProblem(
          "Repository expression must have a 'revision' option, e.g. internal(revision=3,4,5).");
    }

    RevisionHistory rh = repo.revisionHistory();
    ImmutableList.Builder<Revision> revBuilder = ImmutableList.builder();
    for (String revId : repoEx.getOption("revision").split(",")) {
      revBuilder.add(rh.findHighestRevision(revId));
    }
    return revBuilder.build();
  }
}
