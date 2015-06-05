// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.writer;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.io.File;

/**
 * An Writer is the interface to create a revision in MOE.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface Writer {

  /**
   * Makes a draft revision in which the Source Control system behind this Writer contains c.
   *
   * @param c  the Codebase to replicate
   *
   * @returns the draft revision created
   *
   * @throws WritingError if an error occurred
   */
  DraftRevision putCodebase(Codebase c) throws WritingError;

  /**
   * Makes a draft revision in which the Source Control system behind this Writer contains c and
   * metadata for the revision.
   *
   * @param c  the Codebase to replicate
   * @param rm  the RevisionMetadata to include
   *
   * @returns the draft revision created
   *
   * @throws WritingError if an error occurred
   */
  DraftRevision putCodebase(Codebase c, RevisionMetadata rm) throws WritingError;

  /**
   * Returns a conceptual root for the writer.
   */
  File getRoot();

  /**
   * Print out (to Ui) instructions for pushing any changes in this Writer to the remote source.
   */
  void printPushMessage();
}
