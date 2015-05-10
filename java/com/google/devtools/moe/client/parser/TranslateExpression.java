// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.parser;

import com.google.common.base.Preconditions;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.project.ProjectContext;

/**
 * An expression encapsulating the transformation of the given Expression's Codebase via the
 * application of a {@link Translator}. For example,
 * new RepositoryExpression("myRepo").translateTo("public")
 * returns a TranslateExpression for "myRepo>public".
 *
 */
public class TranslateExpression extends AbstractExpression {

  private final Expression exToTranslate;
  private final Operation translateOp;

  TranslateExpression(Expression exToTranslate, Operation translateOp) {
    Preconditions.checkArgument(translateOp.operator == Operator.TRANSLATE);
    this.exToTranslate = exToTranslate;
    this.translateOp = translateOp;
  }

  @Override
  public Codebase createCodebase(ProjectContext context) throws CodebaseCreationError {
    Codebase codebaseToTranslate = exToTranslate.createCodebase(context);
    String toProjectSpace = translateOp.term.identifier;
    TranslatorPath path = new TranslatorPath(
        codebaseToTranslate.getProjectSpace(), toProjectSpace);
    Translator translator = context.translators.get(path);
    if (translator == null) {
      throw new CodebaseCreationError(
          String.format(
              "Could not find translator from project space \"%s\" to \"%s\".\n" +
              "Translators only available for %s",
              codebaseToTranslate.getProjectSpace(), toProjectSpace,
              context.translators.keySet()));
    }

    Ui.Task translateTask =
        Injector.INSTANCE.ui().pushTask(
        "translate",
        String.format(
            "Translating %s from project space \"%s\" to \"%s\"",
            codebaseToTranslate.getPath(), codebaseToTranslate.getProjectSpace(), toProjectSpace));

    Codebase translatedCodebase =
        translator.translate(codebaseToTranslate, translateOp.term.options, context);

    // Don't mark the translated codebase for persistence if it wasn't allocated by the Translator.
    if (translatedCodebase.equals(codebaseToTranslate)) {
      Injector.INSTANCE.ui().popTask(translateTask, translatedCodebase.getPath() + " (unmodified)");
    } else {
      Injector.INSTANCE.ui().popTaskAndPersist(translateTask, translatedCodebase.getPath());
    }
    return translatedCodebase.copyWithExpression(this).copyWithProjectSpace(toProjectSpace);
  }

  /**
   * Returns a new TranslateExpression performing this translation with the given reference
   * to-codebase. This is used by inverse translation, for example when inspecting changes such
   * as renamings in the reference to-codebase for the purpose of inverting those renamings.
   */
  public TranslateExpression withReferenceToCodebase(Expression referenceToCodebase) {
    return withOption("referenceToCodebase", referenceToCodebase.toString());
  }

  /**
   * Returns a new TranslateExpression performing this translation with the given reference
   * from-codebase. This is used by inverse translation when merging two sets of changes, the input
   * codebase and the reference to-codebase, onto a reference from-codebase.
   */
  public TranslateExpression withReferenceFromCodebase(Expression referenceFromCodebase) {
    return withOption("referenceFromCodebase", referenceFromCodebase.toString());
  }

  private TranslateExpression withOption(String key, String value) {
    return new TranslateExpression(
        exToTranslate,
        new Operation(translateOp.operator, translateOp.term.withOption(key, value)));
  }

  @Override
  public String toString() {
    return exToTranslate.toString() + translateOp.toString();
  }
}
