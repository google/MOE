// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;

import dagger.Provides;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class InMemoryProjectContextFactory implements ProjectContextFactory {

  public Map<String, String> projectConfigs;

  @Inject public InMemoryProjectContextFactory() {
    projectConfigs = new HashMap<String, String>();
  }

  @Override
  public ProjectContext makeProjectContext(String configFilename) throws InvalidProject {
    return ProjectContext.makeProjectContextFromConfigText(projectConfigs.get(configFilename));
  }

  /** A Dagger module for binding this implementation of {@link ProjectContextFactory}. */
  @dagger.Module public static class Module {
    @Provides @Singleton public ProjectContextFactory factory(InMemoryProjectContextFactory impl) {
      return impl;
    }
  }
}
