// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

/**
 * One step in translating from one project space to another.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class TranslatorStep {

  public final String name;
  public final Editor editor;

  public TranslatorStep(String name, Editor editor) {
    this.name = name;
    this.editor = editor;
  }

}
