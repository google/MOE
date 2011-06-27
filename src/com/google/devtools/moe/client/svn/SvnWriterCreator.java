// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.writer.WritingError;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.Utils;

import java.io.File;
import java.util.Map;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnWriterCreator implements WriterCreator {

  private final String name;
  private final String url;
  private final String projectSpace;
  private final SvnRevisionHistory revisionHistory;

  public SvnWriterCreator(String name, String url, String projectSpace,
                          SvnRevisionHistory revisionHistory) {
    this.name = name;
    this.url = url;
    this.projectSpace = projectSpace;
    this.revisionHistory = revisionHistory;
  }

  public Writer create(Map<String, String> options) throws WritingError {
    Utils.checkKeys(options, ImmutableSet.of("revision"));
    String revId = options.get("revision");
    Revision r = revisionHistory.findHighestRevision(options.get("revision"));
    File tempDir = AppContext.RUN.fileSystem.getTemporaryDirectory(
        String.format("svn_writer_%s_", r.revId));
    SvnWriter writer = new SvnWriter(url, r, tempDir, projectSpace);
    writer.checkOut();
    return writer;
  }
}
