// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.editors.TranslatorStep;
import com.google.devtools.moe.client.parser.Operation;
import com.google.devtools.moe.client.parser.Operator;
import com.google.devtools.moe.client.parser.Parser;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.Ui;

import java.io.File;
import java.lang.IllegalArgumentException;
import java.util.Map;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Evaluator {

  /**
   * Evaluates a String describing a CodebaseExpression.
   *
   * @param expression  a textual representation of the expression
   * @param context  the context to evaluate in
   */
  public static Codebase parseAndEvaluate(String expression, ProjectContext context)
      throws CodebaseCreationError {
    CodebaseExpression e;
    try {
      e = CodebaseExpression.parse(expression);
    } catch (Parser.ParseError error) {
      throw new CodebaseCreationError(
          String.format("could not parse expression \"%s\": %s", expression, error.getMessage()));
    }
    return evaluate(e, context);
  }

  /**
   * Evaluates a parsed CodebaseExpression.
   *
   * @param e  the expression to evaluate
   * @param context  the context to evaluate in
   */
  public static Codebase evaluate(CodebaseExpression e, ProjectContext context)
      throws CodebaseCreationError {
    Ui.Task topLevelTask = AppContext.RUN.ui.pushTask(
        "evaluating",
        String.format("Evaluating Codebase \"%s\"", e));
    try {
      Codebase initialCodebase = create(e.creator, context.repositories);
      File currentDirectory = initialCodebase.getPath();
      String currentProjectSpace = initialCodebase.getProjectSpace();

      for (Operation op: e.operations) {
        if (op.operator == Operator.EDIT) {
          currentDirectory = edit(currentDirectory, op.term, context.editors);
        } else if (op.operator == Operator.TRANSLATE) {
          currentDirectory = translate(currentDirectory, currentProjectSpace, op.term,
                                       context.translators);
          currentProjectSpace = op.term.identifier;
        } else {
          throw new IllegalArgumentException(
              String.format("Unexpected operator %s in Codebase Expression %s",
                            op.operator, e.toString()));
        }
      }
      Codebase result = new Codebase(currentDirectory, currentProjectSpace, e);
      AppContext.RUN.ui.popTask(topLevelTask, currentDirectory.getAbsolutePath());
      return result;
    } catch (CodebaseCreationError ex) {
      ex.printStackTrace();
      AppContext.RUN.ui.popTask(topLevelTask, "Error");
      throw ex;
    }
  }

  /**
   * Evaluates the creation term of the Expression.
   *
   * @param creator  the creator term we are evaluating
   * @param repositories  the repositories available for creation
   */
  static Codebase create(Term creator, Map<String, Repository> repositories)
      throws CodebaseCreationError {
    Repository r = repositories.get(creator.identifier);
    if (r == null) {
      throw new CodebaseCreationError(String.format("no repository %s", creator.identifier));
    }
    CodebaseCreator cc = r.codebaseCreator;
    if (cc == null) {
      throw new CodebaseCreationError(String.format(
          "repository %s cannot create Codebases", creator.identifier));
    }
    Ui.Task createTask = AppContext.RUN.ui.pushTask(
        "create_codebase",
        String.format("Creating from \"%s\"", creator));
    try {
      Codebase result = cc.create(creator.options);
      AppContext.RUN.ui.popTask(createTask, result.getPath().getAbsolutePath());
      return result;
    } catch (CodebaseCreationError ex) {
      AppContext.RUN.ui.popTask(createTask, "Error");
      throw ex;
    }
  }

  /**
   * Performs the edit described in a Term of the Expression.
   */
  static File edit(File input, Term term, Map<String, Editor> editors)
      throws CodebaseCreationError{
    String editorName = term.identifier;
    Editor editor = editors.get(editorName);
    if (editor == null) {
      throw new CodebaseCreationError(
          String.format("no editor %s", editorName));
    }
    return edit(input, editor);
  }

  /**
   * Invokes an editor, appropriately described to the user.
   */
  static File edit(File input, Editor editor) throws CodebaseCreationError {
    Ui.Task editTask = AppContext.RUN.ui.pushTask(
        "edit",
        String.format(
            "Editing %s with editor \"%s\"",
            input.getAbsolutePath(),
            editor.getDescription()));
    try {
      File editedDirectory = editor.edit(input);
      AppContext.RUN.ui.popTask(editTask, editedDirectory.getAbsolutePath());
      return editedDirectory;
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.popTask(editTask, "Error");
      throw e;
    }
  }

  /**
   * Performs the translation described in a Term of the Expression.
   */
  static File translate(File input, String fromProjectSpace,
                        Term translateTerm,
                        Map<TranslatorPath, Translator> translators) throws CodebaseCreationError {
    TranslatorPath path = new TranslatorPath(fromProjectSpace, translateTerm.identifier);
    Translator translator = translators.get(path);
    if (translator == null) {
      throw new CodebaseCreationError(
          String.format("Could not find translator from project space \"%s\" to \"%s\"",
                        fromProjectSpace, translateTerm.identifier));
    }

    File currentDirectory = input;
    Ui.Task translateTask = AppContext.RUN.ui.pushTask(
        "translate",
        String.format(
            "Translating %s from project space \"%s\" to \"%s\"",
            input.getAbsolutePath(), fromProjectSpace, translateTerm.identifier));
    try {
      for (TranslatorStep s: translator.steps) {
        // TODO(dbentley): there's currently no way to invoke a Translator with options to the
        // sub-editors. That's maybe suboptimal. But for now, there's no way to pass
        // options to editors at all. This latter is more suboptimal, but fortunately shadows
        // the former's suboptimality.
        currentDirectory = edit(currentDirectory, s.editor);
      }
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.popTask(translateTask, "Error");
      throw e;
    }

    AppContext.RUN.ui.popTask(translateTask, currentDirectory.getAbsolutePath());
    return currentDirectory;
  }
}
