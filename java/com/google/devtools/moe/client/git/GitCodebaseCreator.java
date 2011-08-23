// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import com.google.common.base.Supplier;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.repositories.Revision;

import java.io.File;
import java.util.Map;

/**
 * Implementation of CodebaseCreator for Git repos that creates a Codebase at a given revision R by
 * cloning a given local repo at head to R.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitCodebaseCreator implements CodebaseCreator {

  private final Supplier<GitClonedRepository> headCloneSupplier;
  private final GitRevisionHistory revisionHistory;
  private final String projectSpace;

  GitCodebaseCreator(Supplier<GitClonedRepository> headCloneSupplier, 
      GitRevisionHistory revisionHistory, String projectSpace) {
    this.headCloneSupplier = headCloneSupplier;
    this.revisionHistory = revisionHistory;
    this.projectSpace = projectSpace;
  }

  @Override
  public Codebase create(Map<String, String> options) {
    Revision rev = revisionHistory.findHighestRevision(options.get("revision"));
    GitClonedRepository headClone = headCloneSupplier.get();
    File archiveLocation = headClone.archiveAtRevId(rev.revId);
    return new Codebase(
        archiveLocation,
        projectSpace,
        new CodebaseExpression(new Term(headClone.getRepositoryName(), options)));
  }

  @Override
  public String getProjectSpace() {
    return projectSpace;
  }
}
