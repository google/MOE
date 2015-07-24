// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs;

import com.google.common.collect.Sets;
import com.google.devtools.moe.client.CommandRunner.CommandException;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
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
 *
 */
// TODO(user): Make this usable for SVN as well.
public abstract class AbstractDvcsWriter<T extends LocalClone> implements Writer {

  /**
   * The LocalClone in which this writer should make and commit changes.
   */
  protected final T revClone;

  protected AbstractDvcsWriter(T revClone) {
    this.revClone = revClone;
  }

  @Override
  public File getRoot() {
    return revClone.getLocalTempDir();
  }

  /**
   * Returns the regexes, a la
   * {@link com.google.devtools.moe.client.project.RepositoryConfig#getIgnoreFileRes()},
   * of filepaths to ignore in this Writer. For example, an Hg implementation of this method would
   * return the getIgnoreFileRes() in its RepositoryConfig along with any Hg-specific paths in its
   * LocalClone, such as "^.hg/.*". Otherwise, this Writer would attempt to modify hg-metadata
   * files.
   */
  protected abstract List<String> getIgnoreFilePatterns();

  @Override
  public DraftRevision putCodebase(Codebase incomingChangeCodebase) throws WritingError {
    incomingChangeCodebase.checkProjectSpace(revClone.getConfig().getProjectSpace());

    Set<String> codebaseFiles = incomingChangeCodebase.getRelativeFilenames();
    Set<String> writerRepoFiles =
        Utils.filterByRegEx(
            Utils.makeFilenamesRelative(
                Injector.INSTANCE.fileSystem().findFiles(getRoot()), getRoot()),
            getIgnoreFilePatterns());

    Set<String> filesToUpdate = Sets.union(codebaseFiles, writerRepoFiles);

    for (String filename : filesToUpdate) {
      try {
        putFile(filename, incomingChangeCodebase);
      } catch (CommandException e) {
        StringBuilder sb = new StringBuilder("Problem occurred while running '");
        sb.append(e.cmd);
        for (String arg : e.args) {
          sb.append(" ").append(arg);
        }
        sb.append("': ").append(e.stderr);
        throw new MoeProblem(sb.toString());
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
  protected abstract void removeFile(String relativeFilename) throws CommandException;

  /**
   * Runs the DVCS command for registering a modified file, if any.
   */
  protected abstract void modifyFile(String relativeFilename) throws CommandException;

  private void putFile(String relativeFilename, Codebase incomingChangeCodebase)
      throws CommandException {
    FileSystem fs = Injector.INSTANCE.fileSystem();
    File src = incomingChangeCodebase.getFile(relativeFilename);
    File dest = new File(getRoot().getAbsolutePath(), relativeFilename);
    boolean srcExists = fs.exists(src);
    boolean destExists = fs.exists(dest);

    if (!srcExists && !destExists) {
      throw new MoeProblem(
          "Neither src nor dests exists. Unreachable code:\n%s\n%s\n%s",
          relativeFilename,
          src,
          dest);
    }

    if (!srcExists) {
      removeFile(relativeFilename);
      return;
    }

    try {
      fs.makeDirsForFile(dest);
      fs.copyFile(src, dest);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }

    if (destExists) {
      modifyFile(relativeFilename);
    } else {
      addFile(relativeFilename);
    }
  }

  /**
   * Returns whether there are changes in the working copy ({@link #revClone}) to commit. An
   * implementation would use a command like 'git status'.
   */
  protected abstract boolean hasPendingChanges();

  /**
   * Commits changes in the working copy ({@link #revClone}) with the given commit metadata.
   *
   * @param revMetaData  the RevisionMetadata to use in making a commit, for example the changelog
   *                     message
   */
  protected abstract void commitChanges(RevisionMetadata revMetaData) throws CommandException;

  @Override
  public DraftRevision putCodebase(Codebase incomingChangeCodebase, RevisionMetadata revMetaData)
      throws WritingError {
    DraftRevision draftRevision = putCodebase(incomingChangeCodebase);

    if (hasPendingChanges()) {
      try {
        commitChanges(revMetaData);
        Injector.INSTANCE
            .ui()
            .info("Converted draft revision to writer at " + getRoot().getAbsolutePath());
      } catch (CommandException e) {
        throw new WritingError("Error committing: " + e);
      }
    }

    return draftRevision;
  }
}
