// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;
import com.google.devtools.moe.client.writer.WritingError;

import java.io.File;
import java.util.Map;

/**
 * {@link WriterCreator} for svn.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnWriterCreator implements WriterCreator {

  private final RepositoryConfig config;
  private final SvnRevisionHistory revisionHistory;
  private final SvnUtil util;

  public SvnWriterCreator(
      RepositoryConfig config, SvnRevisionHistory revisionHistory, SvnUtil util) {
    this.config = config;
    this.revisionHistory = revisionHistory;
    this.util = util;
  }

  @Override
  public Writer create(Map<String, String> options) throws WritingError {
    Utils.checkKeys(options, ImmutableSet.of("revision"));
    String revId = options.get("revision");
    Revision r = revisionHistory.findHighestRevision(options.get("revision"));
    File tempDir =
        Injector.INSTANCE
            .fileSystem()
            .getTemporaryDirectory(String.format("svn_writer_%s_", r.revId));
    SvnWriter writer = new SvnWriter(config, r, tempDir, util);
    writer.checkOut();
    return writer;
  }
}
