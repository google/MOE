// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.Repository;

import java.util.Map;

/**
 * Creates a {@link ProjectContext} given a context file name.
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
// TODO(cgruber): Move most of the create logic to ProjectConfig, since they're basically accessors
public abstract class ProjectContextFactory {

  private final Repositories repositories;

  public ProjectContextFactory(Repositories repositories) {
    this.repositories = checkNotNull(repositories);
  }

  /**
   * Makes a ProjectContext for this config filename.
   *
   * @param configFilename the name of the holding the config
   *
   * @return the ProjectContext to be used
   */
  public final ProjectContext create(String configFilename) throws InvalidProject {
    ProjectConfig config = loadConfiguration(configFilename);
    return new AutoValue_ProjectContext(
        config,
        buildRepositories(config),
        buildEditors(config),
        buildTranslators(config),
        buildMigrationConfigs(config));
  }

  /**
   * Supplies a ProjectConfig file given a file-name.
   *
   * <p>Implementors might, for example, include factories that load project configs from
   * a filesystem, or from in-memory strings.
   */
  protected abstract ProjectConfig loadConfiguration(String configFilename) throws InvalidProject;

  //TODO(cgruber): Consider making InvalidProject an unchecked exception to eliminate these loops.
  private ImmutableMap<String, Repository> buildRepositories(ProjectConfig config)
      throws InvalidProject {
    ImmutableMap.Builder<String, Repository> builder = ImmutableMap.builder();
    for (Map.Entry<String, RepositoryConfig> entry : config.getRepositoryConfigs().entrySet()) {
      builder.put(entry.getKey(), repositories.create(entry.getKey(), entry.getValue()));
    }
    return builder.build();
  }

  private ImmutableMap<String, Editor> buildEditors(ProjectConfig config) throws InvalidProject {
    ImmutableMap.Builder<String, Editor> builder = ImmutableMap.builder();
    for (Map.Entry<String, EditorConfig> entry : config.getEditorConfigs().entrySet()) {
      builder.put(
          entry.getKey(), ProjectContext.makeEditorFromConfig(entry.getKey(), entry.getValue()));
    }
    return builder.build();
  }

  private ImmutableMap<TranslatorPath, Translator> buildTranslators(ProjectConfig config)
      throws InvalidProject {
    ImmutableMap.Builder<TranslatorPath, Translator> builder = ImmutableMap.builder();
    for (TranslatorConfig translatorConfig : config.getTranslators()) {
      Translator t = ProjectContext.makeTranslatorFromConfig(translatorConfig, config);
      TranslatorPath tPath =
          new TranslatorPath(
              translatorConfig.getFromProjectSpace(), translatorConfig.getToProjectSpace());
      builder.put(tPath, t);
    }
    return builder.build();
  }

  private ImmutableMap<String, MigrationConfig> buildMigrationConfigs(ProjectConfig config) {
    ImmutableMap.Builder<String, MigrationConfig> builder = ImmutableMap.builder();
    for (MigrationConfig migrationConfig : config.getMigrationConfigs()) {
      builder.put(migrationConfig.getName(), migrationConfig);
    }
    return builder.build();
  }
}
