// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.codebase.LocalClone;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;

import java.io.File;
import java.util.Map;

/**
 * CodebaseCreator for DVCSes, implemented by cloning a LocalClone to disk.
 *
 */
public abstract class AbstractDvcsCodebaseCreator implements CodebaseCreator {

  private final Supplier<? extends LocalClone> headCloneSupplier;
  private final RevisionHistory revisionHistory;
  private final String projectSpace;

  /**
   * @param headCloneSupplier  a Supplier of the LocalClone that's archived to create a codebase
   *                           (the Supplier should be memoized since its LocalClone is only read
   *                           and archived)
   * @param revisionHistory    a RevisionHistory for parsing revision IDs at creation
   * @param projectSpace       the project space of created Codebases
   */
  // TODO(user): Find a better semantics for when a Supplier provides a new clone every time,
  // or just one clone via memoization, so that the meaning of headCloneSupplier.get() is clearer.
  public AbstractDvcsCodebaseCreator(
      Supplier<? extends LocalClone> headCloneSupplier,
      RevisionHistory revisionHistory,
      String projectSpace) {
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
  protected abstract LocalClone cloneAtLocalRoot(String localroot);

  @Override
  public Codebase create(Map<String, String> options) throws CodebaseCreationError {
    LocalClone headClone;
    File archiveLocation;
    String localRoot = options.get("localroot");
    if (Strings.isNullOrEmpty(localRoot)) {
      Revision rev = revisionHistory.findHighestRevision(options.get("revision"));
      headClone = headCloneSupplier.get();
      archiveLocation = headClone.archiveAtRevision(rev.revId());
    } else {
      // TODO(user): Archive only (don't clone) if localroot is set.
      headClone = cloneAtLocalRoot(localRoot);
      archiveLocation = headClone.archiveAtRevision(null);
    }

    // Filter files in the codebase by RepositoryConfig#ignoreFileRes.
    Predicate<CharSequence> nonIgnoredFilePred =
        Utils.nonMatchingPredicateFromRes(headClone.getConfig().getIgnoreFileRes());
    Utils.filterFiles(archiveLocation, nonIgnoredFilePred);

    return new Codebase(
        archiveLocation,
        projectSpace,
        new RepositoryExpression(new Term(headClone.getRepositoryName(), options)));
  }
}
