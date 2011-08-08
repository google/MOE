// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.WritingError;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;

import java.io.IOException;
import java.io.File;
import java.util.Set;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class SvnWriter implements Writer {
  final String url;
  final Revision revision;
  final File rootDirectory;
  final String projectSpace;

  public SvnWriter(String url, Revision revision, File tempDir, String projectSpace) {
    this.url = url;
    this.revision = revision;
    this.rootDirectory = tempDir;
    this.projectSpace = projectSpace;
  }

  public File getRoot() {
    return rootDirectory;
  }

  public void checkOut() {
    try {
      SvnRepository.runSvnCommand(
          ImmutableList.of("co", "-r", revision.revId, url, rootDirectory.getAbsolutePath()), "");
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem("Could not check out from svn: " + e.stderr);
    }
  }

  public DraftRevision putCodebase(Codebase c) throws WritingError {
    c.checkProjectSpace(projectSpace);
    Set<String> codebaseFiles = c.getRelativeFilenames();
    Set<String> writerFiles = Utils.filterByRegEx(
        Utils.makeFilenamesRelative(AppContext.RUN.fileSystem.findFiles(rootDirectory),
                                    rootDirectory),
        // Filter out files that either start with .svn or have .svn after a slash.
        "(^|.*/)\\.svn(/.*|$)");
    Set<String> union = Sets.union(codebaseFiles, writerFiles);

    for (String filename : union) {
      putFile(filename, c);
    }

    return new SvnDraftRevision(rootDirectory);
  }

  /**
   * Put file from c into this writer. (Helper function.)
   *
   * @param relativeFilename  the filename to put
   * @param c  the Codebase to take the file from
   */
  void putFile(String relativeFilename, Codebase c) {
    try {
      FileSystem fs = AppContext.RUN.fileSystem;
      File dest = new File(rootDirectory.getAbsolutePath(), relativeFilename);
      File src = c.getFile(relativeFilename);
      boolean srcExists = fs.exists(src);
      boolean destExists = fs.exists(dest);

      boolean srcExecutable = fs.isExecutable(src);
      boolean destExecutable = fs.isExecutable(dest);

      if (!srcExists && !destExists) {
        throw new MoeProblem(
            String.format("Neither src nor dests exists. Unreachable code:\n%s\n%s\n%s",
                          relativeFilename, src, dest));
      }

      if (!srcExists) {
        SvnRepository.runSvnCommand(
            ImmutableList.of("rm", relativeFilename), rootDirectory.getAbsolutePath());
        // TODO(dbentley): handle newly-empty directories
        return;
      }

      try {
        fs.makeDirsForFile(dest);
        fs.copyFile(src, dest);
      } catch (IOException e) {
        throw new MoeProblem(e.getMessage());
      }

      if (!destExists) {
        SvnRepository.runSvnCommand(
            ImmutableList.of("add", "--parents", relativeFilename),
            rootDirectory.getAbsolutePath());
      }

      // TODO(dbentley): mime types for files

      if (destExecutable != srcExecutable) {
        if (srcExecutable) {
          SvnRepository.runSvnCommand(
              ImmutableList.of("propset", "svn:executable", "*", relativeFilename),
              rootDirectory.getAbsolutePath());
        } else {
          SvnRepository.runSvnCommand(
              ImmutableList.of("propdel", "svn:executable", relativeFilename),
              rootDirectory.getAbsolutePath());
        }
      }
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem("problem occurred while running svn: " + e.stderr);
    }
  }
}
