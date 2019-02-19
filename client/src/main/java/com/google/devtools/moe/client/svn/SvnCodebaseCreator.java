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

import com.google.common.base.Predicate;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Utils;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.codebase.CodebaseCreator;
import com.google.devtools.moe.client.codebase.expressions.RepositoryExpression;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Revision;
import java.io.File;
import java.util.Map;

/** {@link CodebaseCreator} for svn. */
public class SvnCodebaseCreator extends CodebaseCreator {

  private final FileSystem filesystem;
  private final String name;
  private final RepositoryConfig config;
  private final SvnRevisionHistory revisionHistory;
  private final SvnUtil util;

  public SvnCodebaseCreator(
      FileSystem filesystem,
      String repositoryName,
      RepositoryConfig config,
      SvnRevisionHistory revisionHistory,
      SvnUtil util) {
    this.filesystem = filesystem;
    this.name = repositoryName;
    this.config = config;
    this.revisionHistory = revisionHistory;
    this.util = util;
  }

  @Override
  public Codebase create(Map<String, String> options) throws CodebaseCreationError {
    String revId = options.get("revision");
    if (revId == null) {
      revId = "HEAD";
    }

    Revision rev = revisionHistory.findHighestRevision(revId);

    File exportPath =
        filesystem.getTemporaryDirectory(String.format("svn_export_%s_%s_", name, rev.revId()));

    try {
      util.runSvnCommand(
          "export", config.getUrl(), "-r", rev.revId(), exportPath.getAbsolutePath());
    } catch (CommandRunner.CommandException e) {
      throw new MoeProblem("could not export from svn: %s", e.getMessage());
    }

    // Filter codebase by ignore_file_res.
    final Predicate<CharSequence> nonIgnoredFilePred =
        Utils.nonMatchingPredicateFromRes(config.getIgnoreFilePatterns());
    Utils.filterFiles(exportPath, nonIgnoredFilePred, filesystem);

    return Codebase.create(
        exportPath, config.getProjectSpace(), new RepositoryExpression(name).withOptions(options));
  }
}
