// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.writer.WritingError;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;

import java.util.Map;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class DummyWriterCreator implements WriterCreator {

  private final String repositoryName;

  public DummyWriterCreator(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  @Override
  public Writer create(Map<String, String> options) throws WritingError {
    return new DummyWriter(repositoryName);
  }
}
