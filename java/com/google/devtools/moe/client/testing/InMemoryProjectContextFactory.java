// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import static com.google.devtools.moe.client.project.ProjectConfig.makeProjectConfigFromConfigText;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;

import dagger.Provides;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A project context factory maintains a set of project configurations in memory.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class InMemoryProjectContextFactory extends ProjectContextFactory {
  // TODO(cgruber): Stop with the visible non-final property.
  @VisibleForTesting public Map<String, String> projectConfigs;

  @Inject
  public InMemoryProjectContextFactory(Repositories repositories) {
    super(repositories);
    projectConfigs = new HashMap<String, String>();
  }

  public InMemoryProjectContextFactory() {
    this(new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory())));
  }

  @Override
  public ProjectConfig loadConfiguration(String configFilename) throws InvalidProject {
    return makeProjectConfigFromConfigText(projectConfigs.get(configFilename));
  }

  /** A Dagger module for binding this implementation of {@link ProjectContextFactory}. */
  @dagger.Module
  public static class Module {
    @Provides
    @Singleton
    public ProjectContextFactory factory(InMemoryProjectContextFactory impl) {
      return impl;
    }
  }
}
