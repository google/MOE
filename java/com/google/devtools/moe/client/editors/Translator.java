// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.project.ProjectContext;

import java.util.Map;

/**
 * An interface for translating a Codebase via a series of steps. This is similar to the
 * {@link Editor} interface, but Translators are used in a particular context, i.e. the migration
 * of one repository's changes into another repository.
 *
 */
public interface Translator {

  /**
   * Translate the given Codebase, and return the result.
   */
  Codebase translate(Codebase toTranslate, Map<String, String> options, ProjectContext context)
      throws CodebaseCreationError;

}
