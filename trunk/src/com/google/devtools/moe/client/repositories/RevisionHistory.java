// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

/**
 * An abstraction of a Repository's Revision History.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface RevisionHistory {

  /**
   * Finds the highest revision less than or equal to revId.
   *
   * @param revId  the id of the revision to start at. Empty string for head.
   *
   * @return the highest Revision less than or equal to revId.
   */
  public Revision findHighestRevision(String revId);
}
