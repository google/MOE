/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.translation.pipeline;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.ProjectContext;
import java.util.List;
import java.util.Map;

/**
 * A TranslationPipeline that translates a Codebase from one project space to another by
 * calling its constituent Editors in turn in translate().
 */
public class ForwardTranslationPipeline implements TranslationPipeline {

  private final Ui ui;
  private final List<TranslationStep> steps;

  public ForwardTranslationPipeline(Ui ui, List<TranslationStep> steps) {
    this.ui = ui;
    this.steps = steps;
  }

  @Override
  public Codebase translate(
      Codebase toTranslate, Map<String, String> options, ProjectContext context) {
    Codebase translated = toTranslate;
    for (TranslationStep s : steps) {
      // TODO(cgruber) use streams here.
      try (Task task = ui.newTask("edit", "Translation editor: " + s.name)) {
        // Pass the translation options to each editor.
        translated = task.keep(s.editor.edit(translated, options));
      }
    }
    return translated;
  }
}
