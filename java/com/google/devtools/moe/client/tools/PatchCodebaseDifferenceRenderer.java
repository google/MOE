/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.tools;

import com.google.common.base.Joiner;

import java.io.File;

/**
 * Renders a CodebaseDifference into a patch file.
 */
public class PatchCodebaseDifferenceRenderer implements CodebaseDifferenceRenderer {

  @Override
  public String render(CodebaseDifference d) {
    StringBuilder r = new StringBuilder();
    Joiner.on(' ').appendTo(r, "diff", d.codebase1.toString(), d.codebase2.toString());
    r.append('\n');
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
            new File(d.codebase1.toString(), fd.relativeFilename()).getPath(),
            new File(d.codebase2.toString(), fd.relativeFilename()).getPath()));

    if (fd.executability() == FileDifference.Comparison.ONLY1) {
      r.append("-mode:executable\n");
    }
    if (fd.executability() == FileDifference.Comparison.ONLY2) {
      r.append("+mode:executable\n");
    }

    r.append(String.format("<<< %s/%s\n", d.codebase1, fd.relativeFilename()));
    r.append(String.format(">>> %s/%s\n", d.codebase2, fd.relativeFilename()));

    // NB(dbentley): For generating a patch, we don't care if the existence of files
    // differs. Why? Because files whose existence differs will almost certainly differ
    // in other ways.
    // TODO(dbentley): what about if we add an empty, unexecutable file? Uhh, hmm....
    // Mercurial seems to not show this diff, so maybe we're all right.

    if (fd.contentDiff() != null) {
      r.append(fd.contentDiff());
      r.append("\n");
    }
  }
}
