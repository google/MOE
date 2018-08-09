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

package com.google.devtools.moe.client.dvcs;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.codebase.LocalWorkspace;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import java.io.File;
import java.util.Map;

/** CodebaseCreator for DVCSes, implemented by cloning a LocalClone to disk. */
public abstract class AbstractDvcsCodebaseCreator extends CodebaseCreator {

  private final Supplier<? extends LocalWorkspace> headCloneSupplier;
  private final RevisionHistory revisionHistory;
  private final String projectSpace;
  protected final CommandRunner cmd;
  protected final FileSystem filesystem;

  /**
   * @param headCloneSupplier a Supplier of the LocalClone that's archived to create a codebase (the
   *     Supplier should be memoized since its LocalClone is only read and archived)
   * @param revisionHistory a RevisionHistory for parsing revision IDs at creation
   * @param projectSpace the project space of created Codebases
   */
  // TODO(user): Find a better semantics for when a Supplier provides a new clone every time,
  // or just one clone via memoization, so that the meaning of headCloneSupplier.get() is clearer.
  public AbstractDvcsCodebaseCreator(
      CommandRunner cmd,
      FileSystem filesystem,
      Supplier<? extends LocalWorkspace> headCloneSupplier,
      RevisionHistory revisionHistory,
      String projectSpace) {
    this.cmd = cmd;
    this.filesystem = filesystem;
    this.headCloneSupplier = headCloneSupplier;
    this.revisionHistory = revisionHistory;
    this.projectSpace = projectSpace;
  }

  /**
   * Clones from a local location, rather than the remote location used in the
   * {@code headCloneSupplier}. This is used, for example, in inverse translation when we want
   * to create a reference to-codebase from a working copy of a repo that includes some local-only
   * changes.
   *
   * @param localroot  the absolute path of the local clone to re-clone
   * @return a LocalClone of the re-clone
   */
  protected abstract LocalWorkspace cloneAtLocalRoot(String localroot);

  @Override
  public Codebase create(Map<String, String> options) throws CodebaseCreationError {
    LocalWorkspace headClone;
    File archiveLocation;
    String localRoot = options.get("localroot");
    if (Strings.isNullOrEmpty(localRoot)) {
      Revision rev;
      try {
        rev = revisionHistory.findHighestRevision(options.get("revision"));
      } catch (MoeProblem e) {
        String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        throw new CodebaseCreationError(e, "%s", message);
      }
      headClone = headCloneSupplier.get();
      archiveLocation = headClone.archiveAtRevision(rev.revId());
    } else {
      // TODO(user): Archive only (don't clone) if localroot is set.
      headClone = cloneAtLocalRoot(localRoot);
      archiveLocation = headClone.archiveAtRevision(null);
    }

    // Filter files in the codebase by RepositoryConfig#ignoreFileRes.
    Predicate<CharSequence> nonIgnoredFilePred =
        Utils.nonMatchingPredicateFromRes(headClone.getConfig().getIgnoreFilePatterns());
    Utils.filterFiles(archiveLocation, nonIgnoredFilePred, filesystem);

    return Codebase.create(
        archiveLocation,
        projectSpace,
        new RepositoryExpression(headClone.getRepositoryName()).withOptions(options));
  }
}
