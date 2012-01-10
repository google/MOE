// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.dvcs.AbstractDvcsWriter;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;

/**
 * Hg implementation of {@link AbstractDvcsWriter}. For migrations, local commits are made on the
 * default branch from last equivalence revision, potentially creating a new head in default.
 *
 */
public class HgWriter extends AbstractDvcsWriter<HgClonedRepository> {

  protected HgWriter(HgClonedRepository revClone) {
    super(revClone);
  }

  @Override
  protected List<String> getIgnoreFilePatterns() {
    return ImmutableList.<String>builder()
        .addAll(revClone.getConfig().getIgnoreFileRes())
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
  protected void rmFile(String relativeFilename) throws CommandException {
    revClone.runHgCommand("rm", relativeFilename);
  }

  @Override
  protected void commitChanges(RevisionMetadata rm) throws CommandException {
    revClone.runHgCommand("commit", "--message", rm.description);
  }

  @Override
  protected boolean hasPendingChanges() {
    String statusStdout = null;
    try {
      statusStdout = revClone.runHgCommand("status");
    } catch (CommandException e) {
      throw new MoeProblem("Error in hg status: " + e);
    }
    return !Strings.isNullOrEmpty(statusStdout);
  }

  @Override
  public void printPushMessage() {
    Ui ui = AppContext.RUN.ui;
    ui.info("=====");
    ui.info("MOE changes have been committed to a clone at " + getRoot());
    ui.info("Changes may have created a new head. Merge heads if needed, then push to remote.");
    ui.info("For example:");
    ui.info("hg heads");
    ui.info("hg merge  # if more than one head");
    ui.info("hg commit -m 'MOE merge'");
    ui.info("hg push");
    ui.info("=====");
  }
}
