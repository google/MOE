// Copyright 2011 Google Inc. All Rights Reserved.

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

  public DummyWriterCreator(String repositoryName) {}

  public Writer create(Map<String, String> options) throws WritingError {
    return new DummyWriter();
  }
}
