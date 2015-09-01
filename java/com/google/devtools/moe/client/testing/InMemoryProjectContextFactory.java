// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import static com.google.devtools.moe.client.project.ProjectConfig.makeProjectConfigFromConfigText;

import com.google.common.annotations.VisibleForTesting;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;

import dagger.Provides;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
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
  public InMemoryProjectContextFactory(
      FileDiffer differ,
      CommandRunner cmd,
      @Nullable FileSystem filesystem,
      Ui ui,
      Repositories repositories) {
    super(differ, cmd, filesystem, ui, repositories);
    projectConfigs = new HashMap<String, String>();
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