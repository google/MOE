// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.codebase.CodebaseExpression;
import com.google.devtools.moe.client.parser.Term;
import com.google.devtools.moe.client.repositories.Revision;

import java.io.File;
import java.util.Map;

/**
 * Implementation of CodebaseCreator for Hg repos that creates a Codebase at a given revision R by
 * cloning a given local repo at tip to R
 *
 */
public class HgCodebaseCreator implements CodebaseCreator {

  private final HgClonedRepository tipClone;
  private final HgRevisionHistory revisionHistory;
  private final String projectSpace;

  HgCodebaseCreator(
      HgClonedRepository tipClone, HgRevisionHistory revisionHistory, String projectSpace) {
    this.tipClone = tipClone;
    this.revisionHistory = revisionHistory;
    this.projectSpace = projectSpace;
  }

  @Override
  public Codebase create(Map<String, String> options) throws CodebaseCreationError {
    Revision rev = revisionHistory.findHighestRevision(options.get("revision"));
    File archiveLocation = tipClone.archiveAtRevId(rev.revId);
    return new Codebase(
        archiveLocation,
        projectSpace,
        new CodebaseExpression(new Term(tipClone.getRepositoryName(), options)));
  }

  @Override
  public String getProjectSpace() {
    return projectSpace;
  }
}
