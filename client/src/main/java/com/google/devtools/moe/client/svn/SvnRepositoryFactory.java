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

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;

import javax.inject.Inject;

/**
 * Creates a Subversion implementation of {@link RepositoryType}.
 */
public class SvnRepositoryFactory implements RepositoryType.Factory {

  private final FileSystem filesystem;
  private final SvnUtil util;
  private final Ui ui;

  @Inject
  public SvnRepositoryFactory(FileSystem filesystem, SvnUtil util, Ui ui) {
    this.filesystem = filesystem;
    this.util = util;
    this.ui = ui;
  }

  @Override
  public String type() {
    return "svn";
  }

  @Override
  public RepositoryType create(String name, RepositoryConfig config) throws InvalidProject {
    checkType(config);

    String url = config.getUrl();
    if (isNullOrEmpty(url)) {
      throw new InvalidProject("Svn repository config missing \"url\".");
    }

    SvnRevisionHistory rh = new SvnRevisionHistory(name, url, util);
    return RepositoryType.create(
        name,
        rh,
        new SvnCodebaseCreator(filesystem, name, config, rh, util),
        new SvnWriterCreator(config, rh, util, filesystem, ui));
  }
}
