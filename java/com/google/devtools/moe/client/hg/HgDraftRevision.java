// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import com.google.common.base.Supplier;
import com.google.devtools.moe.client.writer.DraftRevision;

/**
 * Encapsulates a change in an Hg repo, to be pushed.
 *
 */
public class HgDraftRevision implements DraftRevision {

  private final Supplier<HgClonedRepository> revCloneSupplier;

  HgDraftRevision(Supplier<HgClonedRepository> revCloneSupplier) {
    this.revCloneSupplier = revCloneSupplier;
  }

  @Override
  public String getLocation() {
    return revCloneSupplier.get().getLocalTempDir().getAbsolutePath();
  }
}
