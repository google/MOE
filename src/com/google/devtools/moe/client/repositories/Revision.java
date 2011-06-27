// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.common.base.Objects;

/**
 * A Revision in a source control system.
 *
 * A dumb object with no mutable state.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Revision {

  public final String revId;
  public final String repositoryName;

  public Revision() {
    this.revId = "";
    this.repositoryName = "";
  } // For gson

  public Revision(String revId, String repositoryName) {
    this.revId = revId;
    this.repositoryName = repositoryName;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(repositoryName, revId);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Revision) {
      Revision revisionObj = (Revision) obj;
      return (Objects.equal(repositoryName, revisionObj.repositoryName) &&
              Objects.equal(revId, revisionObj.revId));
    }
    return false;
  }
}
