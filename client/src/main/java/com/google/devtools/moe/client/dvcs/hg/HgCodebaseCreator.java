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

import com.google.common.base.Supplier;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.codebase.LocalWorkspace;
import com.google.devtools.moe.client.dvcs.AbstractDvcsCodebaseCreator;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import java.io.File;

/**
 * Hg implementation of AbstractDvcsCodebaseCreator to handle local cloning.
 */
public class HgCodebaseCreator extends AbstractDvcsCodebaseCreator {

  private final String repositoryName;
  private final RepositoryConfig config;
  private final File hgBinary;
  private final Lifetimes lifetimes;

  public HgCodebaseCreator(
      CommandRunner cmd,
      FileSystem filesystem,
      File hgBinary,
      Supplier<? extends LocalWorkspace> headCloneSupplier,
      RevisionHistory revisionHistory,
      String projectSpace,
      String repositoryName,
      RepositoryConfig config,
      Lifetimes lifetimes) {
    super(cmd, filesystem, headCloneSupplier, revisionHistory, projectSpace);
    this.hgBinary = hgBinary;
    this.repositoryName = repositoryName;
    this.config = config;
    this.lifetimes = lifetimes;
  }

  @Override
  protected LocalWorkspace cloneAtLocalRoot(String localroot) {
    HgClonedRepository clone =
        new HgClonedRepository(
            cmd, filesystem, hgBinary, repositoryName, config, localroot, lifetimes);
    clone.cloneLocallyAtHead(lifetimes.currentTask());
    return clone;
  }
}
