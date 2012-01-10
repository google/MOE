// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.codebase;

/**
 * An error occurred while creating a Codebase.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class CodebaseCreationError extends Exception {

  public CodebaseCreationError(String message) {
    super(message);
  }
}
