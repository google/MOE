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

package com.google.devtools.moe.client.writer;

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * A DraftRevision encapsulates a Revision that was created by MOE.
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
        ui.popTask(t, "");
        return r;
      } catch (WritingError e) {
        throw new MoeProblem(e, "Error creating draft revision to codebase");
      }
    }
  }
}
