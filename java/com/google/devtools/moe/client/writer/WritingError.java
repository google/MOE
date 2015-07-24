// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.writer;

/**
 * An error occurred while MOE was trying to edit code.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class WritingError extends Exception {

  public WritingError(String message) {
    super(message);
  }
}
