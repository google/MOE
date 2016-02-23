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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.dvcs.AbstractDvcsWriter;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;

/**
 * Hg implementation of {@link AbstractDvcsWriter}. For migrations, local commits are made on the
 * default branch from last equivalence revision, potentially creating a new head in default.
 */
public class HgWriter extends AbstractDvcsWriter<HgClonedRepository> {

  protected HgWriter(HgClonedRepository revClone) {
    super(revClone);
  }

  @Override
  protected List<String> getIgnoreFilePatterns() {
    return ImmutableList.<String>builder()
        .addAll(revClone.getConfig().getIgnoreFilePatterns())
        .add("^\\.hg.*")
        .build();
  }

  @Override
  protected void addFile(String relativeFilename) throws CommandException {
    revClone.runHgCommand("add", relativeFilename);
  }

  @Override
  protected void modifyFile(String relativeFilename) throws CommandException {
    // No op.
  }

  @Override
  protected void removeFile(String relativeFilename) throws CommandException {
    revClone.runHgCommand("rm", relativeFilename);
  }

  @Override
  protected void commitChanges(RevisionMetadata rm) throws CommandException {
    List<String> args =
        Lists.newArrayList(
            "commit",
            "--message",
            rm.description,
            "--date",
            rm.date.getMillis() / 1000 + " " + -rm.date.getZone().getOffset(rm.date) / 1000);
    if (rm.author != null) {
      args.add("--user");
      args.add(rm.author);
    }
    revClone.runHgCommand(args.toArray(new String[args.size()]));
  }

  @Override
  protected boolean hasPendingChanges() {
    try {
      String statusStdout = revClone.runHgCommand("status");
      return !Strings.isNullOrEmpty(statusStdout);
    } catch (CommandException e) {
      throw new MoeProblem("Error in hg status: " + e);
    }
  }

  @Override
  public void printPushMessage() {
    Ui ui = Injector.INSTANCE.ui();
    ui.message("=====");
    ui.message("MOE changes have been committed to a clone at %s", getRoot());
    ui.message("Changes may have created a new head. Merge heads if needed, then push to remote.");
    ui.message("For example:");
    ui.message("$ hg heads");
    ui.message("$ hg merge  # if more than one head");
    ui.message("$ hg commit -m 'MOE merge'");
    ui.message("$ hg push");
    ui.message("=====");
  }
}
