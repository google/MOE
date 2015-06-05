// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.ProjectContext;

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
 *
 */
public interface InverseEditor {

  Codebase inverseEdit(
      Codebase input,
      Codebase referenceFrom,
      Codebase referenceTo,
      ProjectContext context,
      Map<String, String> options);
}
