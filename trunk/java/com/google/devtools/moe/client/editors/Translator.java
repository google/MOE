// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.editors;

import java.util.List;

/**
 * A Translator translates a Codebase from one project space to another.
 *
 * For now, it is only the data. This is an odd design decision, I realize.
 *
 * I worry that a lot of the logic (checking input project space, setting output project space)
 * would be needlessly repeated.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class Translator {

  public final String fromProjectSpace;
  public final String toProjectSpace;
  public final List<TranslatorStep> steps;

  public Translator(String fromProjectSpace, String toProjectSpace, List<TranslatorStep> steps) {
    this.fromProjectSpace = fromProjectSpace;
    this.toProjectSpace = toProjectSpace;
    this.steps = steps;
  }

  public TranslatorPath getTranslatorPath() {
    return new TranslatorPath(fromProjectSpace, toProjectSpace);
  }
}
