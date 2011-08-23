// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.git;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Writer implementation for Git. Construct it with an GitClonedRepository at some revision.
 * putCodebase() will modify that clone per the given Codebase (which could be from any repo, Git or
 * not).
 *
 * @author michaelpb@gmail.com (Michael Bethencourt)
 */
public class GitWriter implements Writer {

  private final Supplier<GitClonedRepository> revCloneSupplier;
  private final String projectSpace;

  GitWriter(Supplier<GitClonedRepository> revCloneSupplier, String projectSpace) {
    this.revCloneSupplier = revCloneSupplier;
    this.projectSpace = projectSpace;
  }

  @Override
  public File getRoot() {
    return revCloneSupplier.get().getLocalTempDir();
  }

  @Override
  public DraftRevision putCodebase(Codebase c) {
    c.checkProjectSpace(projectSpace);
    Set<String> codebaseFiles = c.getRelativeFilenames();
    Set<String> writerRepoFiles = Utils.filterByRegEx(
        Utils.makeFilenamesRelative(
            AppContext.RUN.fileSystem.findFiles(getRoot()),
            getRoot()),
        // Filter out paths and files that start with '.git'.
        "^\\.git.*");

    Set<String> union = Sets.union(codebaseFiles, writerRepoFiles);

    for (String filename : union) {
      try {
        putFile(filename, c);
      } catch (CommandException e) {
        throw new MoeProblem("problem occurred while running git: " + e.stderr);
      }
    }

    return new GitDraftRevision(revCloneSupplier);
  }

  @Override
  public DraftRevision putCodebase(Codebase c, RevisionMetadata rm) {
    DraftRevision dr = putCodebase(c);

    // Generate a shell script to commit repo with description and author.
    // TODO(user): Git allows you to specify a --author="user <user@foo.com>" tag when committing
    // but the given author needs to be a valid, registered author of this repo. That is hard to
    // guarantee so, the author is appended to the description for now.
    String message = String.format("git commit -m \"%s\" \ngitq push", (rm.description +
        " (original change by " + rm.author + ")"));
    Utils.makeShellScript(message,
        revCloneSupplier.get().getLocalTempDir().getAbsolutePath() + "/git_commit.sh");

    AppContext.RUN.ui.info(String.format("To commit, run: cd %s && ./git_commit.sh && cd -",
        revCloneSupplier.get().getLocalTempDir().getAbsolutePath()));
    return dr;
  }

  private void putFile(String relativeFilename, Codebase c) throws CommandException {
    FileSystem fs = AppContext.RUN.fileSystem;
    File src = c.getFile(relativeFilename);
    File dest = new File(getRoot().getAbsolutePath(), relativeFilename);
    boolean srcExists = fs.exists(src);
    boolean destExists = fs.exists(dest);

    if (!srcExists && !destExists) {
      throw new MoeProblem(
          String.format("Neither src nor dests exists. Unreachable code:\n%s\n%s\n%s",
                        relativeFilename, src, dest));
    }

    if (!srcExists) {
      revCloneSupplier.get().runGitCommand(ImmutableList.of("rm", relativeFilename));
      return;
    }

    try {
      fs.makeDirsForFile(dest);
      fs.copyFile(src, dest);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }

    if (!destExists) {
      revCloneSupplier.get().runGitCommand(ImmutableList.of("add", relativeFilename));
    }
  }
}
