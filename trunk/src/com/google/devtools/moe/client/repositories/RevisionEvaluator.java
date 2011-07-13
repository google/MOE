// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.RevisionExpression.RevisionExpressionError;

import java.util.List;

/**
 *
 */
public class RevisionEvaluator {

  public static List<Revision> parseAndEvaluate(String exp, ProjectContext context)
      throws RevisionExpressionError {
    RevisionExpression revEx;
    try {
      revEx = RevisionExpression.parse(exp);
    } catch (RevisionExpressionError e) {
      throw new RevisionExpressionError(String.format("Invalid Revision Expression: "
          + "%s \n %s", exp, e.getMessage()));
    }
    return evaluate(revEx, context);
  }

  public static List<Revision> evaluate(RevisionExpression revEx, ProjectContext context)
      throws RevisionExpressionError {
    Repository repo = context.repositories.get(revEx.repoId);
    if (repo == null) {
      throw new RevisionExpressionError("No repository " + revEx.repoId);
    }

    RevisionHistory rh = repo.revisionHistory;
    if (rh == null) {
      throw new RevisionExpressionError("Repository " + repo.name +
          " does not support revision history.");
    }

    ImmutableList.Builder<Revision> builder = ImmutableList.<Revision>builder();
    for (String revId : revEx.revIds) {
      Revision rev = rh.findHighestRevision(revId);
      if (rev.revId.equals(revId)) {
        builder.add(rev);
      } else {
        throw new RevisionExpressionError("Revision \"" + revId +
            "\" does not exist in repository \"" + repo.name + "\"");
      }
    }
    return builder.build();
  }
}
