// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;

/**
 * Represents the fully interpreted project, its textual configurations realized into
 * relevant data structures.
 */
@AutoValue
public abstract class ProjectContext {
  public abstract ProjectConfig config();

  public abstract ImmutableMap<String, RepositoryType> repositories();

  public abstract ImmutableMap<String, Editor> editors();

  public abstract ImmutableMap<TranslatorPath, Translator> translators();

  public abstract ImmutableMap<String, MigrationConfig> migrationConfigs();

  /**
   * Returns the {@link RepositoryType} in this context with the given name.
   *
   * @throws MoeProblem if no such repository with the given name exists
   */
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
