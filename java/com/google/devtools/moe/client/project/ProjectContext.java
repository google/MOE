// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.dvcs.git.GitRepository;
import com.google.devtools.moe.client.dvcs.hg.HgRepository;
import com.google.devtools.moe.client.editors.Editor;
import com.google.devtools.moe.client.editors.ForwardTranslator;
import com.google.devtools.moe.client.editors.IdentityEditor;
import com.google.devtools.moe.client.editors.InverseEditor;
import com.google.devtools.moe.client.editors.InverseRenamingEditor;
import com.google.devtools.moe.client.editors.InverseScrubbingEditor;
import com.google.devtools.moe.client.editors.InverseTranslator;
import com.google.devtools.moe.client.editors.InverseTranslatorStep;
import com.google.devtools.moe.client.editors.PatchingEditor;
import com.google.devtools.moe.client.editors.RenamingEditor;
import com.google.devtools.moe.client.editors.ScrubbingEditor;
import com.google.devtools.moe.client.editors.ShellEditor;
import com.google.devtools.moe.client.editors.Translator;
import com.google.devtools.moe.client.editors.TranslatorPath;
import com.google.devtools.moe.client.editors.TranslatorStep;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.svn.SvnRepository;
import com.google.devtools.moe.client.testing.DummyRepository;

import java.util.List;
import java.util.Map;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ProjectContext {

  public final ProjectConfig config;
  private final ImmutableMap<String, Repository> repositories;
  public final ImmutableMap<String, Editor> editors;
  public final ImmutableMap<TranslatorPath, Translator> translators;
  public final ImmutableMap<String, MigrationConfig> migrationConfigs;

  public ProjectContext(
      ProjectConfig config,
      ImmutableMap<String, Repository> repositories,
      ImmutableMap<String, Editor> editors,
      ImmutableMap<TranslatorPath, Translator> translators,
      ImmutableMap<String, MigrationConfig> migrationConfigs) {
    this.config = config;
    this.repositories = repositories;
    this.editors = editors;
    this.translators = translators;
    this.migrationConfigs = migrationConfigs;
  }

  /**
   * Returns the {@link Repository} in this context with the given name.
   *
   * @throws MoeProblem if no such repository with the given name exists
   */
  public Repository getRepository(String repositoryName) {
    if (!repositories.containsKey(repositoryName)) {
      throw new MoeProblem("No such repository '" + repositoryName + "' in the config. Found: "
          + ImmutableSortedSet.copyOf(repositories.keySet()));
    }
    return repositories.get(repositoryName);
  }

  public static ProjectContext makeProjectContextFromConfigText(String configText)
      throws InvalidProject {
    ProjectConfig config =
        ProjectConfig.makeProjectConfigFromConfigText(configText);

    ImmutableMap.Builder<String, Repository> b = ImmutableMap.builder();
    for (Map.Entry<String, RepositoryConfig> e :
             config.getRepositoryConfigs().entrySet()) {
      b.put(e.getKey(), makeRepositoryFromConfig(e.getKey(), e.getValue()));
    }
    ImmutableMap<String, Repository> repositories = b.build();

    ImmutableMap.Builder<String, Editor> editorsBuilder = ImmutableMap.builder();
    for (Map.Entry<String, EditorConfig> entry :
             config.getEditorConfigs().entrySet()) {
      editorsBuilder.put(entry.getKey(), makeEditorFromConfig(entry.getKey(), entry.getValue()));
    }
    ImmutableMap<String, Editor> editors = editorsBuilder.build();

    ImmutableMap.Builder<TranslatorPath, Translator> translatorsBuilder = ImmutableMap.builder();
    for (TranslatorConfig tc : config.getTranslators()) {
      Translator t = makeTranslatorFromConfig(tc, config);
      TranslatorPath tPath = new TranslatorPath(tc.getFromProjectSpace(), tc.getToProjectSpace());
      translatorsBuilder.put(tPath, t);
    }
    ImmutableMap<TranslatorPath, Translator> translators = translatorsBuilder.build();

    ImmutableMap.Builder<String, MigrationConfig> migrationConfigsBuilder = ImmutableMap.builder();
    for (MigrationConfig mc : config.getMigrationConfigs()) {
      migrationConfigsBuilder.put(mc.getName(), mc);
    }
    ImmutableMap<String, MigrationConfig> migrationConfigs = migrationConfigsBuilder.build();

    return new ProjectContext(config, repositories, editors, translators, migrationConfigs);
  }

  @VisibleForTesting static Repository makeRepositoryFromConfig(
      String repositoryName, RepositoryConfig config) throws InvalidProject {
    if (repositoryName.equals("file")) {
      throw new InvalidProject(
          "Invalid repository name (reserved keyword): \"" + repositoryName + "\"");
    }

    switch (config.getType()) {
      case svn:
        return SvnRepository.makeSvnRepositoryFromConfig(
            repositoryName, config);
      case hg:
        return HgRepository.makeHgRepositoryFromConfig(repositoryName, config);
      case git:
        return GitRepository.makeGitRepositoryFromConfig(repositoryName, config);
      case dummy:
        return DummyRepository.makeDummyRepository(repositoryName, config);
      default:
        throw new InvalidProject(
            "Invalid repository type: \"" + config.getType() + "\"");
    }
  }

  private static Editor makeEditorFromConfig(String editorName,
      EditorConfig config) throws InvalidProject {
    switch (config.getType()) {
      case identity:
        return IdentityEditor.makeIdentityEditor(editorName, config);
      case scrubber:
        return ScrubbingEditor.makeScrubbingEditor(editorName, config);
      case patcher:
        return PatchingEditor.makePatchingEditor(editorName, config);
      case shell:
        return ShellEditor.makeShellEditor(editorName, config);
      case renamer:
        return RenamingEditor.makeRenamingEditor(editorName, config);
      default:
        throw new InvalidProject(
            String.format("Invalid editor type: \"%s\"", config.getType()));
    }
  }

  private static Translator makeTranslatorFromConfig(
      TranslatorConfig transConfig, ProjectConfig projConfig) throws InvalidProject {
    if (transConfig.isInverse()) {
      TranslatorConfig otherTrans = findInverseTranslatorConfig(transConfig, projConfig);
      return new InverseTranslator(makeStepsFromConfigs(otherTrans.getSteps()),
                                   makeInverseStepsFromConfigs(otherTrans.getSteps()));
    } else {
      return new ForwardTranslator(makeStepsFromConfigs(transConfig.getSteps()));
    }
  }

  private static List<TranslatorStep> makeStepsFromConfigs(List<StepConfig> stepConfigs)
      throws InvalidProject {
    ImmutableList.Builder<TranslatorStep> steps = ImmutableList.builder();
    for (StepConfig sc : stepConfigs) {
      steps.add(new TranslatorStep(
          sc.getName(), makeEditorFromConfig(sc.getName(), sc.getEditorConfig())));
    }
    return steps.build();
  }

  private static List<InverseTranslatorStep> makeInverseStepsFromConfigs(
      List<StepConfig> stepConfigs) throws InvalidProject {
    ImmutableList.Builder<InverseTranslatorStep> inverseSteps = ImmutableList.builder();
    for (StepConfig sc : Lists.reverse(stepConfigs)) {
      inverseSteps.add(
          new InverseTranslatorStep(
              "inverse_" + sc.getName(),
              makeInverseEditorFromConfig("inverse_" + sc.getName(), sc.getEditorConfig())));
    }
    return inverseSteps.build();
  }

  private static TranslatorConfig findInverseTranslatorConfig(
      TranslatorConfig transConfig, ProjectConfig projConfig) throws InvalidProject {
    List<TranslatorConfig> otherTranslators = projConfig.getTranslators();
    for (TranslatorConfig otherTrans : otherTranslators) {
      if (otherTrans.getToProjectSpace().equals(transConfig.getFromProjectSpace())
          && otherTrans.getFromProjectSpace().equals(transConfig.getToProjectSpace())) {
        if (otherTrans.isInverse()) {
          throw new InvalidProject("Can't have mutually inverse translators!");
        }
        return otherTrans;
      }
    }
    throw new InvalidProject("Couldn't find translator whose path is inverse of " +
        transConfig.getFromProjectSpace() + " -> " + transConfig.getToProjectSpace());
  }

  private static InverseEditor makeInverseEditorFromConfig(
      String editorName, EditorConfig originalConfig) throws InvalidProject {
    switch (originalConfig.getType()) {
      case identity:
        return IdentityEditor.makeIdentityEditor(editorName, originalConfig);
      case renamer:
        return InverseRenamingEditor.makeInverseRenamingEditor(editorName, originalConfig);
      case scrubber:
        return InverseScrubbingEditor.makeInverseScrubbingEditor();
      default:
        throw new InvalidProject("Non-invertible editor type: " + originalConfig.getType());
    }
  }

  public static class Builder {
    public ProjectConfig config;
    public ImmutableMap<String, Repository> repositories;
    public ImmutableMap<String, Editor> editors;
    public ImmutableMap<TranslatorPath, Translator> translators;
    public ImmutableMap<String, MigrationConfig> migrationConfigs;

    public Builder() {
      config = null;
      repositories = ImmutableMap.of();
      editors = ImmutableMap.of();
      translators = ImmutableMap.of();
      migrationConfigs = ImmutableMap.of();
    }

    public Builder withRepositories(ImmutableMap<String, Repository> repositories) {
      this.repositories = repositories;
      return this;
    }

    public Builder withEditors(ImmutableMap<String, Editor> editors) {
      this.editors = editors;
      return this;
    }

    public Builder withTranslators(ImmutableMap<TranslatorPath, Translator> translators) {
      this.translators = translators;
      return this;
    }

    public Builder withMigrations(ImmutableMap<String, MigrationConfig> migrationConfigs) {
      this.migrationConfigs = migrationConfigs;
      return this;
    }

    public ProjectContext build() {
      return new ProjectContext(config, repositories, editors, translators, migrationConfigs);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
