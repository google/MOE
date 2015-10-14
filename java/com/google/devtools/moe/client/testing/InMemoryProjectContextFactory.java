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