// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.MoeProblem;
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

import java.util.List;

/**
 * Represents the fully interpreted project, its textual configurations realized into
 * relevant data structures.
 */
@AutoValue
public abstract class ProjectContext {
  public abstract ProjectConfig config();

  public abstract ImmutableMap<String, Repository> repositories();

  public abstract ImmutableMap<String, Editor> editors();

  public abstract ImmutableMap<TranslatorPath, Translator> translators();

  public abstract ImmutableMap<String, MigrationConfig> migrationConfigs();

  /**
   * Returns the {@link Repository} in this context with the given name.
   *
   * @throws MoeProblem if no such repository with the given name exists
   */
  public Repository getRepository(String repositoryName) {
    if (!repositories().containsKey(repositoryName)) {
      throw new MoeProblem(
          "No such repository '"
              + repositoryName
              + "' in the config. Found: "
              + ImmutableSortedSet.copyOf(repositories().keySet()));
    }
    return repositories().get(repositoryName);
  }

  static Editor makeEditorFromConfig(String editorName, EditorConfig config) throws InvalidProject {
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
        throw new InvalidProject("Invalid editor type: \"%s\"", config.getType());
    }
  }

  static Translator makeTranslatorFromConfig(TranslatorConfig transConfig, ProjectConfig projConfig)
      throws InvalidProject {
    if (transConfig.isInverse()) {
      TranslatorConfig otherTrans = findInverseTranslatorConfig(transConfig, projConfig);
      return new InverseTranslator(
          makeStepsFromConfigs(otherTrans.getSteps()),
          makeInverseStepsFromConfigs(otherTrans.getSteps()));
    } else {
      return new ForwardTranslator(makeStepsFromConfigs(transConfig.getSteps()));
    }
  }

  private static List<TranslatorStep> makeStepsFromConfigs(List<StepConfig> stepConfigs)
      throws InvalidProject {
    ImmutableList.Builder<TranslatorStep> steps = ImmutableList.builder();
    for (StepConfig sc : stepConfigs) {
      steps.add(
          new TranslatorStep(
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
    throw new InvalidProject(
        "Couldn't find translator whose path is inverse of %s -> %s",
        transConfig.getFromProjectSpace(),
        transConfig.getToProjectSpace());
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
}
