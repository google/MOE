// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.writer.DraftRevision;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DummyDraftRevision implements DraftRevision {

  public DummyDraftRevision() {}

  public String getLocation() {
    return "/dummy/revision";
  }
}
