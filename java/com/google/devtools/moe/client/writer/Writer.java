// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.writer;

import com.google.devtools.moe.client.codebase.Codebase;

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
  public DraftRevision putCodebase(Codebase c) throws WritingError;

  /**
   * Returns a conceptual root for the writer.
   */
  public File getRoot();
}
