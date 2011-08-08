// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.writer;

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
}
