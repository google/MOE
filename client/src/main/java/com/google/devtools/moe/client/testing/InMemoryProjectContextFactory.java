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

import static com.google.devtools.moe.client.project.ProjectConfigs.parse;

import com.google.common.annotations.VisibleForTesting;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.config.ProjectConfig;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.translation.editors.Editors;
import dagger.Binds;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A project context factory maintains a set of project configurations in memory.
 */
public class InMemoryProjectContextFactory extends ProjectContextFactory {
  // TODO(cgruber): Stop with the visible non-final property.
  @VisibleForTesting public Map<String, String> projectConfigs;

  @Inject
  public InMemoryProjectContextFactory(
      ExpressionEngine expressionEngine, Ui ui, Repositories repositories) {
    super(expressionEngine, ui, repositories, new Editors.Fake());
    projectConfigs = new HashMap<>();
  }

  @Override
  public ProjectConfig loadConfiguration(String configFilename) throws InvalidProject {
    return parse(projectConfigs.get(configFilename));
  }

  @Override
  public void loadUsernamesFiles(ProjectConfig config) {
    // no tests using this factory currently use this feature, no-op for now
    return;
  }

  /** A Dagger module for binding this implementation of {@link ProjectContextFactory}. */
  @dagger.Module
  public abstract static class Module {
    @Binds
    @Singleton
    public abstract ProjectContextFactory factory(InMemoryProjectContextFactory impl);
  }
}
