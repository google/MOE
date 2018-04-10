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

package com.google.devtools.moe.client.dvcs.hg;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WriterCreator;
import java.util.Map;

/** An Hg implementation of WriterCreator, which modifies a local {@link HgClonedRepository}. */
public class HgWriterCreator implements WriterCreator {

  private final Supplier<HgClonedRepository> freshCloneSupplier;
  private final HgRevisionHistory revHistory;
  private final FileSystem filesystem;
  private final Ui ui;

  HgWriterCreator(
      Supplier<HgClonedRepository> freshCloneSupplier,
      HgRevisionHistory revHistory,
      FileSystem filesystem,
      Ui ui) {
    this.freshCloneSupplier = freshCloneSupplier;
    this.revHistory = revHistory;
    this.filesystem = filesystem;
    this.ui = ui;
  }

  @Override
  public Writer create(Map<String, String> options) {
    Utils.checkKeys(options, ImmutableSet.of("revision"));
    // Sanity check: make sure the given revision exists.
    Revision rev = revHistory.findHighestRevision(options.get("revision"));
    HgClonedRepository freshClone = freshCloneSupplier.get();
    freshClone.updateToRevision(rev.revId());
    return new HgWriter(freshClone, filesystem, ui);
  }
}
