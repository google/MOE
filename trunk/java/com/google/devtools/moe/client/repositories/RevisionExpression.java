// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.parser.Operation;
import com.google.devtools.moe.client.parser.Operator;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.writer.WriterExpression;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A way to describe one or more revisions in a particular repository.
 *
 * Revision Expressions consist of one or two parts:
 * 1) Repository name: an alphanumeric (underscores allowed) identifier for a repository
 * 2) Revision names(optional): a curly-bracketed lists of one or more revisions separated by commas
 * Ex) "googlecode{1543,1544,1546}", "myrepository{rev22}", "internal"
 *
 * A RevisionExpression with just the repository name defaults to the head revision
 *
 */
public class RevisionExpression {

  public final String repoId;
  public final List<String> revIds;

  public RevisionExpression(String repoId, List<String> revIds) {
    this.repoId = repoId;
    this.revIds = Collections.unmodifiableList(revIds);
  }

  /*
   * This regex will match a RevisionExpression which consists of the following:
   * 1) The name of the repository.
   * 2) An optional curly-bracketed list of one or more revision names separated by commas.
   * Ex) "googlecode{12,13,14}" or "internal{rev3435}" or "myrepo"
   */
  private static final Pattern REV_EXP_REGEX =
      Pattern.compile("(\\w+)(\\{(\\w+\\s*,\\s*)*(\\w+)\\})?");

  /**
   * Takes in a string representing a Revision Expression and returns the corresponding object.
   * @throws RevisionExpressionError
   */
  public static RevisionExpression parse(String text) throws RevisionExpressionError {
    Matcher m = REV_EXP_REGEX.matcher(text);
    if (!m.matches()) {
      throw new RevisionExpressionError("A Revision Expression consists of a repository name "
          + "followed by an optional curly bracket-enclosed, comma-separated list of revision IDs. "
          + "e.g. \"googlecode{3523}\" or \"mercurial{r34,r35,r36}\" or just \"myrepo\"");
    }
    String repoId = m.group(1);
    ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
    if (!(m.group(2) == null)) {
      String revIdString = m.group(2).substring(1, m.group(2).length() - 1);
      for (String revId : Splitter.on(",").split(revIdString)) {
        builder.add(revId.trim());
      }
    }
    return new RevisionExpression(repoId, builder.build());
  }

  @Override
  public String toString() {
    if (!this.revIds.isEmpty()) {
      return this.repoId + "{" + Joiner.on(",").join(revIds) + "}";
    }
    return this.repoId;
  }

  /**
   * Determines whether the given string is a proper Revision Expression.
   */
  public static boolean isValid(String text) {
    return REV_EXP_REGEX.matcher(text).matches();
  }

  /**
   * Change this RevisionExpression into a CodebaseExpression.
   *
   * @param projectSpace  the project space to apply a translator between, or null if none
   * @param context the ProjectContext to make sure Revisions are valid
   *
   * @return  a CodebaseExpression at fromRevision's first revId with translator to projectSpace,
   *          or null on failure
   */
  public CodebaseExpression toCodebaseExpression(String projectSpace, ProjectContext context) {
    List<Revision> revisions;
    try {
      revisions = RevisionEvaluator.evaluate(this, context);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return null;
    }

    Map<String, String> options = revisions.isEmpty() ?
        ImmutableMap.<String, String>of() : ImmutableMap.of("revision", revisions.get(0).revId);

    Term creator = new Term(repoId, options);
    List<Operation> operations;
    if (projectSpace == null) {
      operations = ImmutableList.<Operation>of();
    } else {
      Operator operator = Operator.TRANSLATE;
      Term term = new Term(projectSpace, ImmutableMap.<String, String>of());
      operations = ImmutableList.of(new Operation(operator, term));
    }

    return new CodebaseExpression(creator, operations);
  }

  /**
   * Change this RevisionExpression into a WriterExpression.
   *
   * @param context  the ProjectContext to make sure Revisions are valid
   *
   * @return  a WriterExpression at the first revId, or null on failure
   */
  //TODO(user): should be able to handle more than just the first revId?
  public WriterExpression toWriterExpression(ProjectContext context) {
    List<Revision> revisions;
    try {
      revisions = RevisionEvaluator.evaluate(this, context);
    } catch (RevisionExpressionError e) {
      AppContext.RUN.ui.error(e.getMessage());
      return null;
    }
    Map<String, String> options = revisions.isEmpty() ?
        ImmutableMap.<String, String>of() : ImmutableMap.of("revision", revisions.get(0).revId);

    return new WriterExpression(new Term(repoId, options));
  }

  /**
   * An error occurred with a RevisionExpression.
   */
  public static class RevisionExpressionError extends Exception {

    private String error;

    public RevisionExpressionError(String error) {
      this.error = error;
    }

    @Override
    public String getMessage() {
      return String.format("Revision Expression Error: %s", error);
    }
  }
}
