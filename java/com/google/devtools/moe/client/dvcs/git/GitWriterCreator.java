// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.git;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;

import java.util.Map;

/**
 * A Git implementation of WriterCreator, which modifies a local {@link GitClonedRepository}.
 *
 */
public class GitWriterCreator implements WriterCreator {

  private final Supplier<GitClonedRepository> freshCloneSupplier;
  private final GitRevisionHistory revHistory;

  GitWriterCreator(Supplier<GitClonedRepository> headCloneSupplier, GitRevisionHistory revHistory) {
    this.freshCloneSupplier = headCloneSupplier;
    this.revHistory = revHistory;
  }

  @Override
  public Writer create(Map<String, String> options) {
    Utils.checkKeys(options, ImmutableSet.of("revision"));
    // Sanity check: make sure the given revision exists.
    Revision rev = revHistory.findHighestRevision(options.get("revision"));
    GitClonedRepository freshClone = freshCloneSupplier.get();
    freshClone.updateToRevision(rev.revId());
    return new GitWriter(freshClone);
  }
}
