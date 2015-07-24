// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.svn;

import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.Repository;

import javax.inject.Inject;

/**
 * Creates a Subversion implementation of {@link Repository}.
 */
public class SvnRepositoryFactory implements Repository.Factory {

  private final SvnUtil util;

  @Inject
  public SvnRepositoryFactory(SvnUtil util) {
    this.util = util;
  }

  @Override
  public String type() {
    return "svn";
  }

  @Override
  public Repository create(String name, RepositoryConfig config) throws InvalidProject {
    config.checkType(this);

    String url = config.getUrl();
    if (url == null || url.isEmpty()) {
      throw new InvalidProject("Svn repository config missing \"url\".");
    }

    SvnRevisionHistory rh = new SvnRevisionHistory(name, url, util);
    return Repository.create(
        name,
        rh,
        new SvnCodebaseCreator(name, config, rh, util),
        new SvnWriterCreator(config, rh, util));
  }
}
