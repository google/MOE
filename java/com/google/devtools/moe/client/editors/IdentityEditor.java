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

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.EditorConfig;
import com.google.devtools.moe.client.project.ProjectContext;

import java.util.Map;

/**
 * An IdentityEditor returns the same Codebase.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class IdentityEditor implements Editor, InverseEditor {

  IdentityEditor() {}

  /**
   * Returns a description of what this editor will do.
   */
  @Override
  public String getDescription() {
    return "identity";
  }

  /**
   * Takes in a Codebase and returns it unedited. Not particularly useful.
   */
  @Override
  public Codebase edit(Codebase input, ProjectContext context, Map<String, String> options) {
    return input;
  }

  @Override
  public Codebase inverseEdit(
      Codebase input,
      Codebase referenceFrom,
      Codebase referenceTo,
      ProjectContext context,
      Map<String, String> options) {
    return input;
  }

  public static IdentityEditor makeIdentityEditor(String editorName, EditorConfig editor) {
    return new IdentityEditor();
  }
}
