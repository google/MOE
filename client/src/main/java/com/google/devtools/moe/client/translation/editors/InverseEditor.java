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
package com.google.devtools.moe.client.translation.editors;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.InvalidProject;
import java.util.Map;

/**
 * An InverseEditor takes three codebases and conceptually "merges" their changes together, under
 * the following assumptions:
 *
 * <ul>
 * <li>Forward-editing transforms Codebase referenceTo into Codebase referenceFrom.
 * <li>Codebase input contains new changes to preserve through inverse editing.
 * </ul>
 *
 * Therefore the merge that both undoes editing and preserves the new changes in input looks like:
 *
 * <pre>
 *                referenceFrom == referenceTo|editor
 *                     /                   \
 *               referenceTo      input (referenceFrom + changes)
 *                     \                   /
 *                     referenceTo + changes
 * </pre>
 */
public interface InverseEditor {

  Codebase inverseEdit(
      Codebase input,
      Codebase referenceFrom,
      Codebase referenceTo,
      Map<String, String> options);

  /**
   * Validates that this InverseEditor is properly configured to permit inversion, and returns the
   * validated editor.
   */
  public InverseEditor validateInversion() throws InvalidProject;
}
