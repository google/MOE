/*
 * Copyright (c) 2011 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;
import com.google.devtools.moe.client.writer.WritingError;
import java.io.File;
import java.util.Map;

/** {@link WriterCreator} for svn. */
public class SvnWriterCreator implements WriterCreator {

  private final RepositoryConfig config;
  private final SvnRevisionHistory revisionHistory;
  private final SvnUtil util;
  private final FileSystem fs;
  private final Ui ui;

  public SvnWriterCreator(
      RepositoryConfig config, SvnRevisionHistory revisionHistory, SvnUtil util, FileSystem fs, Ui ui) {
    this.config = config;
    this.revisionHistory = revisionHistory;
    this.util = util;
    this.fs = fs;
    this.ui = ui;
  }

  @Override
  public Writer create(Map<String, String> options) throws WritingError {
    Utils.checkKeys(options, ImmutableSet.of("revision"));
    Revision r = revisionHistory.findHighestRevision(options.get("revision"));
    File tempDir = fs.getTemporaryDirectory(String.format("svn_writer_%s_", r.revId()));
    SvnWriter writer = new SvnWriter(config, r, tempDir, util, fs, ui);
    writer.checkOut();
    return writer;
  }
}
