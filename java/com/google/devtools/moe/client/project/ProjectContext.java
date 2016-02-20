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

package com.google.devtools.moe.client.project;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSortedSet;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;

import java.util.Map;

/**
 * Represents the fully interpreted project, its textual configurations realized into
 * relevant data structures.
 */
@AutoValue
public abstract class ProjectContext {
  public abstract ProjectConfig config();

  public abstract Map<String, RepositoryType> repositories();

  public abstract Map<String, Editor> editors();

  public abstract Map<TranslatorPath, Translator> translators();

  public abstract Map<String, MigrationConfig> migrationConfigs();

  /**
   * Returns the {@link RepositoryType} in this context with the given name.
   *
   * @throws MoeProblem if no such repository with the given name exists
   */
  // TODO(cgruber) Migrate this to a cleaner model, and inject the above five properties
  public RepositoryType getRepository(String repositoryName) {
    if (!repositories().containsKey(repositoryName)) {
      throw new MoeProblem(
          "No such repository '"
              + repositoryName
              + "' in the config. Found: "
              + ImmutableSortedSet.copyOf(repositories().keySet()));
    }
    return repositories().get(repositoryName);
  }
}
