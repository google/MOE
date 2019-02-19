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

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;
import javax.inject.Inject;

/** Creates a Git implementation of {@link RepositoryType}. */
public class GitRepositoryFactory implements RepositoryType.Factory {
  private final CommandRunner cmd;
  private final FileSystem filesystem;
  private final Ui ui;
  private final Lifetimes lifetimes;

  @Inject
  public GitRepositoryFactory(
      CommandRunner cmd, FileSystem filesystem, Ui ui, Lifetimes lifetimes) {
    this.cmd = cmd;
    this.filesystem = filesystem;
    this.ui = ui;
    this.lifetimes = lifetimes;
  }

  @Override
  public String type() {
    return "git";
  }

  /**
   * Create a Repository from a RepositoryConfig indicating an Git repo ("type" == "git").
   *
   * @throws InvalidProject if RepositoryConfig is missing a repo URL.
   */
  @Override
  public RepositoryType create(final String name, final RepositoryConfig config)
      throws InvalidProject {
    checkType(config);

    final String url = config.getUrl();
    if (isNullOrEmpty(url)) {
      throw new InvalidProject("Git repository config missing \"url\".");
    }

    Supplier<GitClonedRepository> freshSupplier =
        () -> {
          GitClonedRepository headClone =
              new GitClonedRepository(cmd, filesystem, name, config, lifetimes);
          headClone.cloneLocallyAtHead(lifetimes.currentTask());
          return headClone;
        };

    // RevisionHistory and CodebaseCreator don't modify their clones, so they can use a shared,
    // memoized supplier.
    Supplier<GitClonedRepository> memoizedSupplier =
        Suppliers.memoize(
            () -> {
              GitClonedRepository tipClone =
                  new GitClonedRepository(cmd, filesystem, name, config, lifetimes);
              tipClone.cloneLocallyAtHead(lifetimes.moeExecution());
              return tipClone;
            });

    GitRevisionHistory rh = new GitRevisionHistory(memoizedSupplier);

    String projectSpace = config.getProjectSpace();
    if (projectSpace == null) {
      projectSpace = "public";
    }

    GitCodebaseCreator cc =
        new GitCodebaseCreator(
            cmd, filesystem, memoizedSupplier, rh, projectSpace, name, config, lifetimes);

    GitWriterCreator wc = new GitWriterCreator(freshSupplier, rh, filesystem, ui);

    return RepositoryType.create(name, rh, cc, wc);
  }
}
