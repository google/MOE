// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.writer;

import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.Ui;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class WriterEvaluator {

  public static Writer parseAndEvaluate(String expression, ProjectContext context)
      throws WritingError {
    WriterExpression e;
    try {
      e = WriterExpression.parse(expression);
    } catch (Parser.ParseError error) {
      throw new WritingError(
          String.format("could not parse expression \"%s\": %s", expression, error.getMessage()));
    }
    return evaluate(e, context);
  }

  public static Writer evaluate(WriterExpression e, ProjectContext context)
      throws WritingError {
    Repository r = context.repositories.get(e.creator.identifier);
    if (r == null) {
      throw new WritingError(String.format("no repository %s", e.creator.identifier));
    }
    WriterCreator ec = r.writerCreator;
    if (ec == null) {
      throw new WritingError(String.format(
          "repository %s cannot create Writers", e.creator.identifier));
    }
    Ui.Task t = AppContext.RUN.ui.pushTask(
        "create_writer",
        String.format("Creating Writer \"%s\"", e));
    Writer result = null;
    try {
      result = ec.create(e.creator.options);
      return result;
    } finally {
      AppContext.RUN.ui.popTask(
          t,
          (result == null) ? "Error" : result.getRoot().getAbsolutePath());
    }
  }
}
