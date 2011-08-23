// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import com.google.common.base.Supplier;
import com.google.devtools.moe.client.writer.DraftRevision;

/**
 * Encapsulates a change in an Git repo, to be pushed.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitDraftRevision implements DraftRevision {

  private final Supplier<GitClonedRepository> revCloneSupplier;

  GitDraftRevision(Supplier<GitClonedRepository> revCloneSupplier) {
    this.revCloneSupplier = revCloneSupplier;
  }

  @Override
  public String getLocation() {
    return revCloneSupplier.get().getLocalTempDir().getAbsolutePath();
  }
}
