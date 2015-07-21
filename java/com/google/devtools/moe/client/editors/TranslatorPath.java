// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

/**
 * The Path a Translator should take.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
// TODO(cgruber) @AutoValue
public class TranslatorPath {

  public final String fromProjectSpace;
  public final String toProjectSpace;

  public TranslatorPath(String fromProjectSpace, String toProjectSpace) {
    this.fromProjectSpace = fromProjectSpace;
    this.toProjectSpace = toProjectSpace;
  }

  @Override
  public String toString() {
    return String.format("%s>%s", fromProjectSpace, toProjectSpace);
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TranslatorPath)) {
      return false;
    }
    return toString().equals(o.toString());
  }
}
