// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import javax.annotation.Nullable;

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
    return change(c, destination, null);
  }

  /**
   * Create a change with metadata in a source control system
   *
   * @param c the Codebase to use as source
   * @param destination the Writer to put the files from c into
   * @param rm the metadata associated with this change
   *
   * @return a DraftRevision on success, or null on failure
   */
  public static DraftRevision change(
      Codebase c, Writer destination, @Nullable RevisionMetadata rm) {
    DraftRevision r;
    try {
      Ui.Task t =
          Injector.INSTANCE.ui().pushTask(
          "push_codebase",
          "Putting files from Codebase into Writer");
      r = (rm == null) ? destination.putCodebase(c) : destination.putCodebase(c, rm);
      Injector.INSTANCE.ui().popTask(t, "");
      return r;
    } catch (WritingError e) {
      Injector.INSTANCE.ui().error(e, "Error writing change");
    }
    return null;
  }
}
