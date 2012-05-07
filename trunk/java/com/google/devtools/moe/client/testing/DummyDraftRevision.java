// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.writer.DraftRevision;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DummyDraftRevision implements DraftRevision {

  private final String repositoryName;

  public DummyDraftRevision(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  @Override
  public String getLocation() {
    return "/dummy/revision/" + repositoryName;
  }
}
