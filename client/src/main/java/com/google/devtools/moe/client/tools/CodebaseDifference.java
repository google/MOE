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

import com.google.devtools.moe.client.codebase.Codebase;
import java.util.Collections;
import java.util.Set;

/**
 * Describes the difference between two Codebases.
 */
public class CodebaseDifference {

  public final Codebase codebase1;
  public final Codebase codebase2;
  public final Set<FileDifference> fileDiffs;

  public CodebaseDifference(Codebase codebase1, Codebase codebase2, Set<FileDifference> fileDiffs) {
    this.codebase1 = codebase1;
    this.codebase2 = codebase2;
    this.fileDiffs = Collections.unmodifiableSet(fileDiffs);
  }

  /**
   * Return whether the Codebases are different.
   */
  public boolean areDifferent() {
    return !this.fileDiffs.isEmpty();
  }


}
