// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs;

import com.google.devtools.moe.client.codebase.LocalWorkspace;
import com.google.devtools.moe.client.writer.DraftRevision;

/**
 * A DraftRevision for DVCSes, i.e. a clone to disk with locally staged or committed changes.
 *
 */
public class DvcsDraftRevision implements DraftRevision {

  private final LocalWorkspace revClone;

  public DvcsDraftRevision(LocalWorkspace revClone) {
    this.revClone = revClone;
  }

  @Override
  public String getLocation() {
    return revClone.getLocalTempDir().getAbsolutePath();
  }
}
