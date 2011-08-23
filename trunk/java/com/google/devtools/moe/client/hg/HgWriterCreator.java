// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.hg;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;
import com.google.devtools.moe.client.writer.WritingError;

import java.util.Map;

/**
 * WriterCreator implementation for Hg. Construct it with an HgClonedRepository, and create() will
 * update it to a given revision and return an HgWriter for it.
 *
 */
public class HgWriterCreator implements WriterCreator {

  private final Supplier<HgClonedRepository> tipCloneSupplier;
  private final HgRevisionHistory revHistory;
  private final String projectSpace;

  HgWriterCreator(Supplier<HgClonedRepository> tipCloneSupplier, HgRevisionHistory revHistory,
      String projectSpace) {
    this.tipCloneSupplier = tipCloneSupplier;
    this.revHistory = revHistory;
    this.projectSpace = projectSpace;
  }

  @Override
  public Writer create(Map<String, String> options) throws WritingError {
    Utils.checkKeys(options, ImmutableSet.of("revision"));
    // Sanity check: make sure the given revision exists.
    Revision rev = revHistory.findHighestRevision(options.get("revision"));
    tipCloneSupplier.get().updateToRevId(rev.revId);
    return new HgWriter(tipCloneSupplier, projectSpace);
  }
}
