// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.ProjectContext;

import java.util.List;
import java.util.Map;

/**
 * A Translator that translates a Codebase from one project space to another by calling its
 * constituent Editors in turn in translate().
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ForwardTranslator implements Translator {

  private final List<TranslatorStep> steps;

  public ForwardTranslator(List<TranslatorStep> steps) {
    this.steps = steps;
  }

  @Override
  public Codebase translate(
      Codebase toTranslate, Map<String, String> options, ProjectContext context) {
    Codebase translated = toTranslate;
    for (TranslatorStep s : steps) {
      Ui.Task editTask = AppContext.RUN.ui.pushTask("edit", "Translation editor: " + s.name);
      // Pass the translation options to each editor.
      translated = s.editor.edit(translated, context, options);
      AppContext.RUN.ui.popTaskAndPersist(editTask, translated.getPath());
    }
    return translated;
  }
}
