// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

/**
 * One step in inverse-translating from one project space to another.
 *
 */
public class InverseTranslatorStep {

  private final String name;
  private final InverseEditor inverseEditor;

  public InverseTranslatorStep(String name, InverseEditor inverseEditor) {
    this.name = name;
    this.inverseEditor = inverseEditor;
  }

  public String getName() {
    return name;
  }

  public InverseEditor getInverseEditor() {
    return inverseEditor;
  }
}
