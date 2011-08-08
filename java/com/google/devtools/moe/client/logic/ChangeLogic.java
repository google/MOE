// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

/**
 * Perform the change directive
 *
 */
public class ChangeLogic {

  /**
   * Create a change in a source control system
   *
   * @param c the Codebase to use as source
   * @param destination the Writer to put the files from c into
   *
   * @return a DraftRevision on success, or null on failure
   */
  public static DraftRevision change(Codebase c, Writer destination) {
    DraftRevision r;
    try {
      Ui.Task t = AppContext.RUN.ui.pushTask(
          "push_codebase",
          "Putting files from Codebase into Writer");
      r = destination.putCodebase(c);
      AppContext.RUN.ui.popTask(t, "");
      return r;
    } catch (WritingError e) {
      AppContext.RUN.ui.error(e.getMessage());
    }
    return null;
  }
}
