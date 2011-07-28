// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import com.google.devtools.moe.client.writer.DraftRevision;

/**
 * Encapsulates a change in an Hg repo, to be pushed.
 *
 */
public class HgDraftRevision implements DraftRevision {

  private final HgClonedRepository revClone;

  HgDraftRevision(HgClonedRepository revClone) {
    this.revClone = revClone;
  }

  @Override
  public String getLocation() {
    return revClone.getLocalTempDir().getAbsolutePath();
  }
}
