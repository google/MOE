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

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.translation.editors.Editor;
import java.util.Map;

/**
 * An interface for translating a Codebase via a series of steps. This is similar to the
 * {@link Editor} interface, but Translators are used in a particular context, i.e. the migration
 * of one repository's changes into another repository.
 */
public interface TranslationPipeline {

  /**
   * Translates the given Codebase, and returns the result.
   */
  Codebase translate(Codebase toTranslate, Map<String, String> options, ProjectContext context)
      throws CodebaseCreationError;
}
