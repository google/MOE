// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.writer;

import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * A DraftRevision encapsulates a Revision that was created by MOE.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface DraftRevision {

  /**
   * Get a description of where this DraftRevision can most easily be seen.
   * It may be a path on the file system, or a link to a web-based code review tool.
   * It has no semantic meaning, but should be useful to a user.
   */
  public String getLocation();

  // TODO(dbentley): Spec out other methods. E.g., Diff, Link, ChangesMade

  /**
   * Creates a {@link DraftRevision} in a given {@link Codebase}.
   *
   */
  public static class Factory {
    private final Ui ui;

    @Inject
    public Factory(Ui ui) {
      this.ui = ui;
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
    public DraftRevision create(Codebase c, Writer destination, @Nullable RevisionMetadata rm) {
      try {
        Ui.Task t = ui.pushTask("push_codebase", "Putting files from Codebase into Writer");
        DraftRevision r = destination.putCodebase(c, rm);
        Injector.INSTANCE.ui().popTask(t, "");
        return r;
      } catch (WritingError e) {
        Injector.INSTANCE.ui().error(e, "Error writing change");
        return null;
      }
    }
  }
}
