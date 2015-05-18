// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.repositories;

import com.google.auto.value.AutoValue;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.writer.WriterCreator;

/**
 * A Repository holds the three abstractions that MOE may need.
 *
 * @author dbentley@google.com (Daniel Bentley)
 * @author cgruber@google.com (Christian Gruber)
 */
@AutoValue
public abstract class Repository {
  public abstract String name();
  public abstract RevisionHistory revisionHistory();
  public abstract CodebaseCreator codebaseCreator();
  public abstract WriterCreator writerCreator();

  public static Repository create(
      String name,
      RevisionHistory revisionHistory,
      CodebaseCreator codebaseCreator,
      WriterCreator writerCreator) {
    return new AutoValue_Repository(name, revisionHistory, codebaseCreator, writerCreator);
  }
}
