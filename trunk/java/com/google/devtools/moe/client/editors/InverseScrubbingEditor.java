// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.editors;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseMerger;
import com.google.devtools.moe.client.project.ProjectContext;

import java.util.Map;

/**
 * An editor that inverts scrubbing via merging.
 *
 * <p>Say a repository 'internal' is translated to 'public' by scrubbing. Say there is an
 * equivalence internal(x) == public(y), where x and y are revision numbers. We want to port a
 * change public(y+1) by inverse-scrubbing to produce internal(x+1). We do this by merging two
 * sets of changes onto public(y):
 *
 * <ol>
 * <li>internal(x), which change represents the addition of all scrubbed content
 * <li>public(y+1), which is the new public change to apply to the internal codebase
 * </ol>
 *
 * <p>The result of 'merge internal(x) public(y) public(y+1)' is the combined addition of scrubbed
 * content and the new public change. This merge produces internal(x+1).
 *
 */
public class InverseScrubbingEditor implements InverseEditor {

  public static InverseScrubbingEditor makeInverseScrubbingEditor() {
    return new InverseScrubbingEditor();
  }


  private InverseScrubbingEditor() {}

  @Override
  public Codebase inverseEdit(Codebase input, Codebase referenceFrom, Codebase referenceTo,
      ProjectContext context, Map<String, String> options) {
    CodebaseMerger merger = new CodebaseMerger(referenceFrom, input, referenceTo);
    return merger.merge();
  }
}
