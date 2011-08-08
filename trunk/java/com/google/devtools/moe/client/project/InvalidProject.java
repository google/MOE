// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class InvalidProject extends Exception {
  public final String explanation;

  public InvalidProject(String explanation) {
    this.explanation = explanation;
  }
}
