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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.config.EditorConfig;
import com.google.devtools.moe.client.config.ProjectConfig;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.config.StepConfig;
import com.google.devtools.moe.client.config.TranslatorConfig;
import com.google.devtools.moe.client.config.MigrationConfig;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.translation.editors.Editor;
import com.google.devtools.moe.client.translation.editors.Editors;
import com.google.devtools.moe.client.translation.pipeline.ForwardTranslationPipeline;
import com.google.devtools.moe.client.translation.pipeline.InverseTranslationPipeline;
import com.google.devtools.moe.client.translation.pipeline.InverseTranslationStep;
import com.google.devtools.moe.client.translation.pipeline.TranslationPath;
import com.google.devtools.moe.client.translation.pipeline.TranslationPipeline;
import com.google.devtools.moe.client.translation.pipeline.TranslationStep;
import java.util.List;
import java.util.Map;

/**
 * Creates a {@link ProjectContext} given a context file name.
 */
// TODO(cgruber): Move most of the create logic to ProjectConfig, since they're basically accessors
public abstract class ProjectContextFactory {

  private final ExpressionEngine expressionEngine;
  private final Repositories repositories;
  private final Editors editors;

  protected final Ui ui;

  public ProjectContextFactory(
      ExpressionEngine expressionEngine, Ui ui, Repositories repositories, Editors editors) {
    // TODO(cgruber):push nullability back from this point.
    this.expressionEngine = expressionEngine;
    this.repositories = Preconditions.checkNotNull(repositories);
    this.ui = ui;
    this.editors = editors;
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
    loadUsernamesFiles(config);
    return ProjectContext.create(
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
  /**
   * Load usernames file if specified in ScrubberConfig and updates ProjectConfig with usernames.
   */
  protected abstract void loadUsernamesFiles(ProjectConfig config);

  //TODO(cgruber): Consider making InvalidProject an unchecked exception to eliminate these loops.
  private ImmutableMap<String, RepositoryType> buildRepositories(ProjectConfig config)
      throws InvalidProject {
    ImmutableMap.Builder<String, RepositoryType> builder = ImmutableMap.builder();

    for (Map.Entry<String, RepositoryConfig> entry : config.repositories().entrySet()) {
      builder.put(entry.getKey(), repositories.create(entry.getKey(), entry.getValue()));
    }
    return builder.build();
  }

  private ImmutableMap<String, Editor> buildEditors(ProjectConfig config) throws InvalidProject {
    ImmutableMap.Builder<String, Editor> builder = ImmutableMap.builder();
    for (Map.Entry<String, EditorConfig> entry : config.editors().entrySet()) {
      builder.put(entry.getKey(), editors.makeEditorFromConfig(entry.getKey(), entry.getValue()));
    }
    return builder.build();
  }

  private ImmutableMap<TranslationPath, TranslationPipeline> buildTranslators(ProjectConfig config)
      throws InvalidProject {
    ImmutableMap.Builder<TranslationPath, TranslationPipeline> builder = ImmutableMap.builder();
    for (TranslatorConfig translatorConfig : config.translators()) {
      TranslationPipeline t = makeTranslatorFromConfig(translatorConfig, config);

      TranslationPath tPath =
          TranslationPath.create(
              translatorConfig.getFromProjectSpace(), translatorConfig.getToProjectSpace());
      builder.put(tPath, t);
    }
    return builder.build();
  }

  private ImmutableMap<String, MigrationConfig> buildMigrationConfigs(ProjectConfig config) {
    ImmutableMap.Builder<String, MigrationConfig> builder = ImmutableMap.builder();
    for (MigrationConfig migrationConfig : config.migrations()) {
      builder.put(migrationConfig.getName(), migrationConfig);
    }
    return builder.build();
  }

  TranslationPipeline makeTranslatorFromConfig(TranslatorConfig transConfig, ProjectConfig projConfig)
      throws InvalidProject {
    if (transConfig.isInverse()) {
      TranslatorConfig otherTrans = findInverseTranslatorConfig(transConfig, projConfig);
      return new InverseTranslationPipeline(
          ui,
          expressionEngine,
          makeStepsFromConfigs(otherTrans.getSteps()),
          makeInverseStepsFromConfigs(otherTrans.getSteps()));
    } else {
      return new ForwardTranslationPipeline(ui, makeStepsFromConfigs(transConfig.getSteps()));
    }
  }

  private List<TranslationStep> makeStepsFromConfigs(List<StepConfig> stepConfigs)
      throws InvalidProject {
    ImmutableList.Builder<TranslationStep> steps = ImmutableList.builder();
    for (StepConfig sc : stepConfigs) {
      steps.add(
          new TranslationStep(
              sc.getName(), editors.makeEditorFromConfig(sc.getName(), sc.getEditorConfig())));
    }
    return steps.build();
  }

  private List<InverseTranslationStep> makeInverseStepsFromConfigs(List<StepConfig> stepConfigs)
      throws InvalidProject {
    ImmutableList.Builder<InverseTranslationStep> inverseSteps = ImmutableList.builder();
    for (StepConfig sc : Lists.reverse(stepConfigs)) {
      inverseSteps.add(
          new InverseTranslationStep(
              "inverse_" + sc.getName(),
              editors.makeInverseEditorFromConfig(
                  "inverse_" + sc.getName(), sc.getEditorConfig())));
    }
    return inverseSteps.build();
  }

  private TranslatorConfig findInverseTranslatorConfig(
      TranslatorConfig transConfig, ProjectConfig projConfig) throws InvalidProject {
    List<TranslatorConfig> otherTranslators = projConfig.translators();
    for (TranslatorConfig otherTrans : otherTranslators) {
      if (otherTrans.getToProjectSpace().equals(transConfig.getFromProjectSpace())
          && otherTrans.getFromProjectSpace().equals(transConfig.getToProjectSpace())) {
        if (otherTrans.isInverse()) {
          throw new InvalidProject("Can't have mutually inverse translators!");
        }
        return otherTrans;
      }
    }
    throw new InvalidProject(
        "Couldn't find translator whose path is inverse of %s -> %s",
        transConfig.getFromProjectSpace(),
        transConfig.getToProjectSpace());
  }

}
