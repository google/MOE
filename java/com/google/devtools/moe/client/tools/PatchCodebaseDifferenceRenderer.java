// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.tools;

import java.io.File;

/**
 * Renders a CodebaseDifference into a patch file.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class PatchCodebaseDifferenceRenderer implements CodebaseDifferenceRenderer {

  public String render(CodebaseDifference d) {
    StringBuilder r = new StringBuilder();

    r.append(String.format("diff %s %s\n", d.codebase1.toString(), d.codebase2.toString()));

    for (FileDifference fd : d.fileDiffs) {
      renderFileDifferenceToStringBuilder(d, fd, r);
    }

    return r.toString();
  }

  /* package */ void renderFileDifferenceToStringBuilder(
      CodebaseDifference d, FileDifference fd, StringBuilder r) {

    r.append(
        String.format(
            "diff --moe %s %s\n",
            new File(d.codebase1.toString(), fd.relativeFilename).getPath(),
            new File(d.codebase2.toString(), fd.relativeFilename).getPath()));

    if (fd.executability == FileDifference.Comparison.ONLY1) {
      r.append("-mode:executable\n");
    }
    if (fd.executability == FileDifference.Comparison.ONLY2) {
      r.append("+mode:executable\n");
    }

    r.append(String.format("<<< %s/%s\n", d.codebase1.toString(), fd.relativeFilename));
    r.append(String.format(">>> %s/%s\n", d.codebase2.toString(), fd.relativeFilename));

    // NB(dbentley): For generating a patch, we don't care if the existence of files
    // differs. Why? Because files whose existence differs will almost certainly differ
    // in other ways.
    // TODO(dbentley): what about if we add an empty, unexecutable file? Uhh, hmm....
    // Mercurial seems to not show this diff, so maybe we're all right.

    if (fd.contentDiff != null) {
      r.append(fd.contentDiff);
      r.append("\n");
    }
  }
}
