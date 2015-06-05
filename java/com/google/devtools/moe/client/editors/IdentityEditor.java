// Copyright 2011 The MOE Authors All Rights Reserved.

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
