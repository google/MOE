// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;

import javax.inject.Inject;

/**
 * Creates a Subversion implementation of {@link RepositoryType}.
 */
public class SvnRepositoryFactory implements RepositoryType.Factory {

  private final FileSystem filesystem;
  private final SvnUtil util;

  @Inject
  public SvnRepositoryFactory(FileSystem filesystem, SvnUtil util) {
    this.filesystem = filesystem;
    this.util = util;
  }

  @Override
  public String type() {
    return "svn";
  }

  @Override
  public RepositoryType create(String name, RepositoryConfig config) throws InvalidProject {
    config.checkType(this);

    String url = config.getUrl();
    if (isNullOrEmpty(url)) {
      throw new InvalidProject("Svn repository config missing \"url\".");
    }

    SvnRevisionHistory rh = new SvnRevisionHistory(name, url, util);
    return RepositoryType.create(
        name,
        rh,
        new SvnCodebaseCreator(filesystem, name, config, rh, util),
        new SvnWriterCreator(config, rh, util));
  }
}
