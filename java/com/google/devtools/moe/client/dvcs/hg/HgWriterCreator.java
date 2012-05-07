// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;

import java.util.Map;

/**
 * An Hg implementation of WriterCreator, which modifies a local {@link HgClonedRepository}.
 *
 */
public class HgWriterCreator implements WriterCreator {

  private final Supplier<HgClonedRepository> freshCloneSupplier;
  private final HgRevisionHistory revHistory;

  HgWriterCreator(Supplier<HgClonedRepository> freshCloneSupplier, HgRevisionHistory revHistory) {
    this.freshCloneSupplier = freshCloneSupplier;
    this.revHistory = revHistory;
  }

  @Override
  public Writer create(Map<String, String> options) {
    Utils.checkKeys(options, ImmutableSet.of("revision"));
    // Sanity check: make sure the given revision exists.
    Revision rev = revHistory.findHighestRevision(options.get("revision"));
    HgClonedRepository freshClone = freshCloneSupplier.get();
    freshClone.updateToRevision(rev.revId);
    return new HgWriter(freshClone);
  }
}
