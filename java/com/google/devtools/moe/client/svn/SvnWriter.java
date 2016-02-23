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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * {@link Writer} for svn.
 */
public class SvnWriter implements Writer {

  private final RepositoryConfig config;
  private final Revision revision;
  private final File rootDirectory;
  private final SvnUtil util;

  public SvnWriter(RepositoryConfig config, Revision revision, File tempDir, SvnUtil util) {
    this.config = config;
    this.revision = revision;
    this.rootDirectory = tempDir;
    this.util = util;
  }

  @Override
  public File getRoot() {
    return rootDirectory;
  }

  public void checkOut() {
    try {
      util.runSvnCommand(
          "co", "-r", revision.revId(), config.getUrl(), rootDirectory.getAbsolutePath());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem("Could not check out from svn: " + e.stderr);
    }
  }

  // TODO(user): Handle separate_revisions! (an 'svn commit' per exported change)
  private DraftRevision putCodebase(Codebase c) {
    c.checkProjectSpace(config.getProjectSpace());

    // Filter out files that either start with .svn or have .svn after a slash, plus the repo
    // config's ignore_file_res.
    List<String> ignoreFilePatterns =
        ImmutableList.<String>builder()
            .addAll(config.getIgnoreFilePatterns())
            .add("(^|.*/)\\.svn(/.*|$)")
            .build();

    Set<String> codebaseFiles = c.getRelativeFilenames();
    Set<String> writerFiles =
        Utils.filterByRegEx(
            Utils.makeFilenamesRelative(
                Injector.INSTANCE.fileSystem().findFiles(rootDirectory), rootDirectory),
            ignoreFilePatterns);
    Set<String> union = Sets.union(codebaseFiles, writerFiles);

    for (String filename : union) {
      putFile(filename, c);
    }

    return new SvnDraftRevision(rootDirectory);
  }

  @Override
  public DraftRevision putCodebase(Codebase c, @Nullable RevisionMetadata rm) throws WritingError {
    DraftRevision dr = putCodebase(c);
    if (rm != null) {
      // Generate a shell script to commit repo with author and description
      String script =
          String.format(
              "svn update%n"
                  + "svn commit -m \"%s\"%n"
                  + "svn propset -r HEAD svn:author \"%s\" --revprop",
              rm.description,
              rm.author);
      Utils.makeShellScript(script, rootDirectory.getAbsolutePath() + "/svn_commit.sh");

      Injector.INSTANCE
          .ui()
          .message(
              "To submit, run: cd %s && ./svn_commit.sh && cd -", rootDirectory.getAbsolutePath());
    }
    return dr;
  }

  /**
   * Put file from c into this writer. (Helper function.)
   *
   * @param relativePath the filename to put
   * @param codebase the Codebase to take the file from
   */
  void putFile(String relativePath, Codebase codebase) {
    try {
      FileSystem fs = Injector.INSTANCE.fileSystem();
      File dest = new File(rootDirectory.getAbsolutePath(), relativePath);
      File src = codebase.getFile(relativePath);
      boolean srcExists = fs.exists(src);
      boolean destExists = fs.exists(dest);

      boolean srcExecutable = fs.isExecutable(src);
      boolean destExecutable = fs.isExecutable(dest);

      if (!srcExists && !destExists) {
        throw new MoeProblem(
            "Neither src nor dests exists. Unreachable code:%n%s%n%s%n%s", relativePath, src, dest);
      }

      if (!srcExists) {
        util.runSvnCommandWithWorkingDirectory(rootDirectory.getAbsolutePath(), "rm", relativePath);
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
        util.runSvnCommandWithWorkingDirectory(
            rootDirectory.getAbsolutePath(), "add", "--parents", relativePath);
      }

      String mimeType = guessMimeType(relativePath);
      if (mimeType != null) {
        try {
          util.runSvnCommandWithWorkingDirectory(
              rootDirectory.getAbsolutePath(), "propset", "svn:mime-type", mimeType, relativePath);
        } catch (CommandRunner.CommandException e) {
          // If the mime type setting fails, it's not really a big deal.
          // Just log it and keep going.
          Injector.INSTANCE.ui().message("Error setting mime-type for %s", relativePath);
        }
      }

      if (destExecutable != srcExecutable) {
        if (srcExecutable) {
          util.runSvnCommandWithWorkingDirectory(
              rootDirectory.getAbsolutePath(), "propset", "svn:executable", "*", relativePath);
        } else {
          util.runSvnCommandWithWorkingDirectory(
              rootDirectory.getAbsolutePath(), "propdel", "svn:executable", relativePath);
        }
      }
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem("problem occurred while running svn: " + e.stderr);
    }
  }

  private String guessMimeType(String relativeFilename) {
    if (relativeFilename.endsWith(".js")) {
      return "text/javascript";
    } else if (relativeFilename.endsWith(".css")) {
      return "text/css";
    } else if (relativeFilename.endsWith(".html")) {
      return "text/html";
    } else if (relativeFilename.endsWith(".jpg")) {
      return "image/jpeg";
    } else if (relativeFilename.endsWith(".png")) {
      return "image/png";
    } else if (relativeFilename.endsWith(".gif")) {
      return "image/gif";
    }
    return null;
  }

  @Override
  public void printPushMessage() {
    // TODO(user): Figure out workflow for MOE migrations/local commits in svn.
  }
}
