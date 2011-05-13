// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.project.EditorConfig;

import java.io.File;

/**
 * An IdentityEditor returns the same Codebase.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class IdentityEditor implements Editor {

  IdentityEditor() {}

  /**
   * Returns a description of what this editor will do.
   */
  public String getDescription() {
    return "identity";
  }

  /**
   * Edits a Directory (returning a new Directory, because modifying in-place is bad).
   *
   * @param input  the directory to edit
   */
  public File edit(File input) throws CodebaseCreationError {
    return input;
  }

  public static IdentityEditor makeIdentityEditor(String editorName, EditorConfig config) {
    return new IdentityEditor();
  }

}
