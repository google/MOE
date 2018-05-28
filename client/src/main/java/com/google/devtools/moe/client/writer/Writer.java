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

import static java.util.Arrays.asList;

import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Keepable;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import javax.annotation.Nullable;

/** An Writer is the interface to create a revision in MOE. */
public interface Writer extends Keepable<Writer> {
  /**
   * Makes a draft revision in which the Source Control system behind this Writer contains c and
   * (optionally) metadata for the revision.
   *
   * @param c  the Codebase to replicate
   * @param rm  the RevisionMetadata to include
   *
   * @returns the draft revision created
   *
   * @throws WritingError if an error occurred
   */
  DraftRevision putCodebase(Codebase c, @Nullable RevisionMetadata rm) throws WritingError;

  /**
   * Returns a conceptual root for the writer.
   */
  File getRoot();

  @Override
  default Collection<Path> toKeep() {
    return asList(getRoot().toPath());
  }

  /** Print out (to Ui) instructions for pushing any changes in this Writer to the remote source. */
  void printPushMessage(Ui ui);
}
