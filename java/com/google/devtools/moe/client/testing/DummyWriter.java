// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import java.io.File;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DummyWriter implements Writer {

  private final String repositoryName;

  DummyWriter(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  @Override
  public DraftRevision putCodebase(Codebase c, RevisionMetadata rm) throws WritingError {
    return new DummyDraftRevision(repositoryName);
  }

  @Override
  public File getRoot() {
    return new File("/dummy/writer/" + repositoryName);
  }

  @Override
  public void printPushMessage() {
    // No op.
  }
}
