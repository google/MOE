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

package com.google.devtools.moe.client.dvcs.git;

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
        .addAll(revClone.getConfig().getIgnoreFilePatterns())
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
    List<String> args =
        Lists.newArrayList(
            "commit", "--all", "--message", rm.description, "--date", rm.date.toString());
    if (rm.author != null) {
      args.add("--author");
      args.add(rm.author);
    }
    revClone.runGitCommand(args.toArray(new String[args.size()]));
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

    Ui ui = Injector.INSTANCE.ui();
    ui.message("=====");
    ui.message("MOE changes have been committed to a clone at " + getRoot());
    if (moeBranchName.startsWith(GitClonedRepository.MOE_MIGRATIONS_BRANCH_PREFIX)) {
      ui.message("Changes are on a new branch. Rebase or merge these changes back onto ");
      ui.message("the desired branch before pushing. For example:");
      ui.message("$ git rebase " + originalBranchName);
      ui.message("$ git checkout " + originalBranchName);
      ui.message("$ git merge --ff-only " + moeBranchName);
      ui.message("$ git push");
    } else {
      ui.message("Changes are on branch '" + moeBranchName + "' and are ready to push.");
    }
    ui.message("=====");
  }
}
