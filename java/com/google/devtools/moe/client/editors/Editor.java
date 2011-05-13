// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;

import java.io.File;

/**
 * An Editor takes one Codebase and returns an edited Codebase.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface Editor {

  /**
   * Returns a description of what this editor will do.
   */
  public String getDescription();

  /**
   * Edits a Directory's contents (not in-place).
   *
   * @param input  the Directory to edit
   *
   * @returns a directory containing the edited contents
   */
  public File edit(File input) throws CodebaseCreationError;

  // TODO(dbentley): this should probably take a Map<String, String> options
}
