// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.writer;

import java.util.Map;

/**
 * An WriterCreator is the interface for creating changes in a Repository.
 * It allows us to create an Writer (which is good for creating one revision).
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface WriterCreator {

  /**
   * Create an Writer against this Repository.
   *
   * @param options  options to create this writer. E.g., revision to check out at, or username/
   *                 password.
   *
   * @return the Writer to use
   *
   * @throw EditingError if we cannot create the Writer
   */
  public Writer create(Map<String, String> options) throws WritingError;
}
