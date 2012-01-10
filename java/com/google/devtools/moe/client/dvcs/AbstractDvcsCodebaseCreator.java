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
   * Construct a DvcsCodebaseCreator with a Supplier of some AbstractDvcsClonedRepository. This
   * Supplier should be memoized (i.e. we need only clone to disk once across all get()s) since
   * CodebaseCreator doesn't modify a clone -- it merely exports it at some revision.
   */
  // TODO(user): Find a better semantics for when a Supplier provides a new clone every time,
  // or just one clone via memoization, so that the meaning of headCloneSupplier.get() is clearer.
  public AbstractDvcsCodebaseCreator(Supplier<? extends LocalClone> headCloneSupplier,
                                     RevisionHistory revisionHistory,
                                     String projectSpace) {
    this.headCloneSupplier = headCloneSupplier;
    this.revisionHistory = revisionHistory;
    this.projectSpace = projectSpace;
  }

  protected abstract LocalClone cloneAtLocalRoot(String localroot);

  @Override
  public Codebase create(Map<String, String> options) throws CodebaseCreationError {
    File archiveLocation;
    LocalClone headClone;
    if (Strings.isNullOrEmpty(options.get("localroot"))) {
      Revision rev = revisionHistory.findHighestRevision(options.get("revision"));
      headClone = headCloneSupplier.get();
      archiveLocation = headClone.archiveAtRevId(rev.revId);
    } else {
      // TODO(user): Archive only (don't clone) if localroot is set.
      headClone = cloneAtLocalRoot(options.get("localroot"));
      archiveLocation = headClone.archiveAtRevId(null);
    }

    // Filter codebase by ignore_file_res.
    final Predicate<CharSequence> nonIgnoredFilePred =
        Utils.nonMatchingPredicateFromRes(headClone.getConfig().getIgnoreFileRes());
    Utils.filterFiles(archiveLocation, nonIgnoredFilePred);

    return new Codebase(
        archiveLocation,
        projectSpace,
        new RepositoryExpression(new Term(headClone.getRepositoryName(), options)));
  }
}
