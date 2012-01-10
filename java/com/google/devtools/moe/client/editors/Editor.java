// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.ProjectContext;

import java.util.Map;

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
   * Takes in a Codebase and returns a new, edited version of that Codebase.
   *
   * @param input  the Codebase to edit
   * @param context  the ProjectContext for this Editor's project
   * @param options  command-line parameters
   * @return a new Codebase that is an edited version of the input
   */
  public Codebase edit(Codebase input, ProjectContext context, Map<String, String> options);
}
