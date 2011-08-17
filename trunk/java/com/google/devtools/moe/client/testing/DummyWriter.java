// Copyright 2011 Google Inc. All Rights Reserved.

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

  @Override
  public DraftRevision putCodebase(Codebase c) throws WritingError {
    return new DummyDraftRevision();
  }

  @Override
  public DraftRevision putCodebase(Codebase c, RevisionMetadata rm) throws WritingError {
    return new DummyDraftRevision();
  }

  public File getRoot() {
    return new File("/foo");
  }
}
