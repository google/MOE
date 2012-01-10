// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs;

import com.google.devtools.moe.client.codebase.LocalClone;
import com.google.devtools.moe.client.writer.DraftRevision;

/**
 * A DraftRevision for DVCSes, i.e. a clone to disk with locally staged or committed changes.
 *
 */
public class DvcsDraftRevision implements DraftRevision {

  private final LocalClone revClone;

  public DvcsDraftRevision(LocalClone revClone) {
    this.revClone = revClone;
  }

  @Override
  public String getLocation() {
    return revClone.getLocalTempDir().getAbsolutePath();
  }
}
