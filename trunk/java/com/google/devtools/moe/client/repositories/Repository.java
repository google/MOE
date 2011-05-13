// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.writer.WriterCreator;

/**
 * A Repository holds the three abstractions that MOE may need.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Repository {

  public final String name;
  public final RevisionHistory revisionHistory;
  public final CodebaseCreator codebaseCreator;
  public final WriterCreator writerCreator;

  public Repository(String name, RevisionHistory revisionHistory, CodebaseCreator codebaseCreator,
                    WriterCreator writerCreator) {
    this.name = name;
    this.revisionHistory = revisionHistory;
    this.codebaseCreator = codebaseCreator;
    this.writerCreator = writerCreator;
  }
}
