// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs;

import com.google.common.collect.Sets;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.LocalClone;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A Writer for DVCSes. Subclasses should implement file modification commands such as add, rm, and
 * commit.
 *
 * @param <T> the type of LocalClone, so that subclasses can use its native methods
 */
// TODO(user): Make this usable for SVN as well.
public abstract class AbstractDvcsWriter<T extends LocalClone> implements Writer {

  protected final T revClone;

  protected AbstractDvcsWriter(T revClone) {
    this.revClone = revClone;
  }

  @Override
  public File getRoot() {
    return revClone.getLocalTempDir();
  }

  protected abstract List<String> getIgnoreFilePatterns();

  @Override
  public DraftRevision putCodebase(Codebase c) throws WritingError {
    c.checkProjectSpace(revClone.getConfig().getProjectSpace());

    Set<String> codebaseFiles = c.getRelativeFilenames();
    Set<String> writerRepoFiles = Utils.filterByRegEx(
        Utils.makeFilenamesRelative(
            AppContext.RUN.fileSystem.findFiles(getRoot()),
            getRoot()),
        getIgnoreFilePatterns());

    Set<String> union = Sets.union(codebaseFiles, writerRepoFiles);

    for (String filename : union) {
      try {
        putFile(filename, c);
      } catch (CommandException e) {
        throw new MoeProblem("problem occurred while running '" + e.cmd + "': " + e.stderr);
      }
    }

    return new DvcsDraftRevision(revClone);
  }

  /**
   * Runs the DVCS command for adding a new file, e.g. 'git add'.
   */
  protected abstract void addFile(String relativeFilename) throws CommandException;

  /**
   * Runs the DVCS command for removing a file, e.g. 'git rm'.
   */
  protected abstract void rmFile(String relativeFilename) throws CommandException;

  /**
   * Runs the DVCS command for registering a modified file, if any.
   */
  protected abstract void modifyFile(String relativeFilename) throws CommandException;

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
      rmFile(relativeFilename);
      return;
    }

    try {
      fs.makeDirsForFile(dest);
      fs.copyFile(src, dest);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }

    if (!destExists) {
      addFile(relativeFilename);
    } else {
      modifyFile(relativeFilename);
    }
  }

  protected abstract boolean hasPendingChanges();

  protected abstract void commitChanges(RevisionMetadata rm) throws CommandException;

  @Override
  public DraftRevision putCodebase(Codebase c, RevisionMetadata rm) throws WritingError {
    DraftRevision dr = putCodebase(c);

    if (hasPendingChanges()) {
      try {
        commitChanges(rm);
        AppContext.RUN.ui.info("Converted draft revision to writer at " +
            getRoot().getAbsolutePath());
      } catch (CommandException e) {
        throw new WritingError("Error committing: " + e);
      }
    }

    return dr;
  }
}
