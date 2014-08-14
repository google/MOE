// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.git;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.dvcs.AbstractDvcsWriter;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;

/**
 * Git implementation of {@link AbstractDvcsWriter}. Writes migrated commits to a
 * {@link GitClonedRepository} on its configured branch or on a branch from last equivalence
 * revision (if the configured branch has moved past equivalence).
 */
public class GitWriter extends AbstractDvcsWriter<GitClonedRepository> {

  GitWriter(GitClonedRepository revClone) {
    super(revClone);
  }

  @Override
  protected List<String> getIgnoreFilePatterns() {
    return ImmutableList.<String>builder()
        .addAll(revClone.getConfig().getIgnoreFileRes())
        .add("^\\.git.*")
        .build();    
  }

  @Override
  protected void addFile(String relativeFilename) throws CommandException {
    revClone.runGitCommand("add", "-f", relativeFilename);
  }
  
  @Override
  protected void modifyFile(String relativeFilename) throws CommandException {
    // Put the modification in the git index.
    revClone.runGitCommand("add", "-f", relativeFilename);
  }
  
  @Override
  protected void removeFile(String relativeFilename) throws CommandException {
    revClone.runGitCommand("rm", relativeFilename);
  }
  
  @Override
  protected void commitChanges(RevisionMetadata rm) throws CommandException {
    List<String> args = Lists.newArrayList("commit", "--all", "--message", rm.description);
    if (revClone.getConfig().getPreserveAuthors()) {
      args.add("--author");
      args.add(rm.author);
    }
    revClone.runGitCommand(args.toArray(new String[0]));
  }
  
  @Override
  protected boolean hasPendingChanges() {
    // NB(yparghi): There may be a simpler way to do this, e.g. git diff or git commit --dry-run
    // using exit codes, but those appear flaky and this is the only reliable way I've found.
    try {
      return !Strings.isNullOrEmpty(revClone.runGitCommand("status", "--short"));
    } catch (CommandException e) {
      throw new MoeProblem("Error in git status: " + e);
    }
  }

  @Override
  public void printPushMessage() {
    String originalBranchName = revClone.getConfig().getBranch().or("master");
    String moeBranchName;
    try {
      moeBranchName = revClone.runGitCommand("rev-parse", "--abbrev-ref", "HEAD").trim();
    } catch (CommandException e) {
      throw new MoeProblem("'git' command error: " + e);
    }

    Ui ui = AppContext.RUN.ui;
    ui.info("=====");
    ui.info("MOE changes have been committed to a clone at " + getRoot());
    if (moeBranchName.startsWith(GitClonedRepository.MOE_MIGRATIONS_BRANCH_PREFIX)) {
      ui.info("Changes are on a new branch. Rebase or merge these changes back onto ");
      ui.info("the desired branch before pushing. For example:");
      ui.info("$ git rebase " + originalBranchName);
      ui.info("$ git checkout " + originalBranchName);
      ui.info("$ git merge --ff-only " + moeBranchName);
      ui.info("$ git push");
    } else {
      ui.info("Changes are on branch '" + moeBranchName + "' and are ready to push.");
    }
    ui.info("=====");
  }
}
