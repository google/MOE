// Copyright 2011 Google Inc. All Rights Reserved.

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
