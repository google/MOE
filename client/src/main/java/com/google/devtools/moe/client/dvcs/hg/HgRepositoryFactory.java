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
import java.io.File;
import javax.inject.Inject;
import javax.inject.Named;

/** Creates a Mercurial (hg) implementation of {@link RepositoryType}. */
public class HgRepositoryFactory implements RepositoryType.Factory {
  private final CommandRunner cmd;
  private final FileSystem filesystem;
  private final File hgBinary;
  private final Ui ui;
  private final Lifetimes lifetimes;

  @Inject
  HgRepositoryFactory(
      CommandRunner cmd,
      FileSystem filesystem,
      @Named("hg_binary") File hgBinary,
      Ui ui,
      Lifetimes lifetimes) {
    this.cmd = cmd;
    this.filesystem = filesystem;
    this.hgBinary = hgBinary;
    this.ui = ui;
    this.lifetimes = lifetimes;
  }

  @Override
  public String type() {
    return "hg";
  }

  /**
   * Create a Repository from a RepositoryConfig indicating an Hg repo ("type" == "hg").
   *
   * @throws InvalidProject if RepositoryConfig is missing a repo URL.
   */
  @Override
  public RepositoryType create(final String name, final RepositoryConfig config)
      throws InvalidProject {
    checkType(config);

    final String url = config.getUrl();
    if (isNullOrEmpty(url)) {
      throw new InvalidProject("Hg repository config missing \"url\".");
    }

    Supplier<HgClonedRepository> freshSupplier =
        () -> {
          HgClonedRepository tipClone =
              new HgClonedRepository(cmd, filesystem, hgBinary, name, config, lifetimes);
          tipClone.cloneLocallyAtHead(lifetimes.currentTask());
          return tipClone;
        };

    // RevisionHistory and CodebaseCreator don't modify their clones, so they can use a shared,
    // memoized supplier.
    Supplier<HgClonedRepository> memoizedSupplier =
        Suppliers.memoize(
            () -> {
              HgClonedRepository tipClone =
                  new HgClonedRepository(cmd, filesystem, hgBinary, name, config, lifetimes);
              tipClone.cloneLocallyAtHead(lifetimes.moeExecution());
              return tipClone;
            });

    HgRevisionHistory rh = new HgRevisionHistory(cmd, hgBinary, memoizedSupplier);

    String projectSpace = config.getProjectSpace();
    if (projectSpace == null) {
      projectSpace = "public";
    }

    HgCodebaseCreator cc =
        new HgCodebaseCreator(
            cmd, filesystem, hgBinary, memoizedSupplier, rh, projectSpace, name, config, lifetimes);

    HgWriterCreator wc = new HgWriterCreator(freshSupplier, rh, filesystem, ui);

    return RepositoryType.create(name, rh, cc, wc);
  }
}
