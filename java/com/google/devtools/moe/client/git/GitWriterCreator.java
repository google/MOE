// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;

import java.util.Map;

/**
 * WriterCreator implementation for Git. Construct it with an GitClonedRepository, and create() will
 * update it to a given revision and return an GitWriter for it.
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitWriterCreator implements WriterCreator {

  private final Supplier<GitClonedRepository> headCloneSupplier;
  private final GitRevisionHistory revHistory;
  private final String projectSpace;

  GitWriterCreator(Supplier<GitClonedRepository> headCloneSupplier, GitRevisionHistory revHistory, 
      String projectSpace) {
    this.headCloneSupplier = headCloneSupplier;
    this.revHistory = revHistory;
    this.projectSpace = projectSpace;
  }

  @Override
  public Writer create(Map<String, String> options) {
    Utils.checkKeys(options, ImmutableSet.of("revision"));
    // Sanity check: make sure the given revision exists.
    Revision rev = revHistory.findHighestRevision(options.get("revision"));
    headCloneSupplier.get().updateToRevId(rev.revId);
    return new GitWriter(headCloneSupplier, projectSpace);
  }
}
