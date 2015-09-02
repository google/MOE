// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.devtools.moe.client.MoeModule;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a MOE Project
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ProjectConfig {
  private String name;
  private Map<String, RepositoryConfig> repositories;
  private Map<String, EditorConfig> editors;
  private List<TranslatorConfig> translators;

  @SerializedName("migrations")
  private List<MigrationConfig> migrationConfigs;

  @SerializedName("internal_repository")
  private RepositoryConfig internalRepository;

  @SerializedName("public_repository")
  private RepositoryConfig publicRepository;

  private ProjectConfig() {} // Constructed by gson

  public String getName() {
    return name;
  }

  /**
   * Returns a mapping of {@link RepositoryConfig} by name in this config. Useful for inspection of
   * this config's contents.
   */
  Map<String, RepositoryConfig> repositories() {
    Preconditions.checkNotNull(repositories);
    return Collections.unmodifiableMap(repositories);
  }

  /**
   * Returns the {@link RepositoryConfig} in this config with the given name.
   *
   * @throws MoeProblem if no such repository with the given name exists
   */
  public RepositoryConfig getRepositoryConfig(String repositoryName) {
    if (!repositories.containsKey(repositoryName)) {
      throw new MoeProblem(
          "No such repository '"
              + repositoryName
              + "' in the config. Found: "
              + ImmutableSortedSet.copyOf(repositories.keySet()));
    }
    return repositories.get(repositoryName);
  }

  public Map<String, EditorConfig> getEditorConfigs() {
    if (editors == null) {
      editors = ImmutableMap.<String, EditorConfig>of();
    }
    return Collections.unmodifiableMap(editors);
  }

  public List<TranslatorConfig> getTranslators() {
    if (translators == null) {
      translators = ImmutableList.of();
    }
    return Collections.unmodifiableList(translators);
  }

  public List<MigrationConfig> getMigrationConfigs() {
    if (migrationConfigs == null) {
      migrationConfigs = ImmutableList.of();
    }
    return Collections.unmodifiableList(migrationConfigs);
  }


  public ScrubberConfig findScrubberConfig(String fromRepository, String toRepository) {
    String fromProjectSpace = getRepositoryConfig(fromRepository).getProjectSpace();
    String toProjectSpace = getRepositoryConfig(toRepository).getProjectSpace();
    for (TranslatorConfig translator : getTranslators()) {
      if (translator.getSteps() != null
          && translator.getFromProjectSpace().equals(fromProjectSpace)
          && translator.getToProjectSpace().equals(toProjectSpace)) {
        for (StepConfig step : translator.getSteps()) {
          if (step.getEditorConfig().getType() == EditorType.scrubber) {
            return step.getEditorConfig().getScrubberConfig();
          }
        }
        break;
      }
    }
    return null;
  }

  void validate() throws InvalidProject {
    if (repositories == null) {
      repositories = Maps.newHashMap();
    }

    if (internalRepository != null) {
      // For backwards compatibility with old MOE configs,
      // normalize the internal repostiory.
      InvalidProject.assertTrue(
          repositories.put("internal", internalRepository) == null,
          "Internal repository specified twice");

      internalRepository = null;
    }

    if (publicRepository != null) {
      // For backwards compatibility with old MOE configs,
      // normalize the public repostiory.
      InvalidProject.assertTrue(
          repositories.put("public", publicRepository) == null,
          "Public repository specified twice");

      publicRepository = null;
    }

    InvalidProject.assertFalse(Strings.isNullOrEmpty(getName()), "Must specify a name");
    InvalidProject.assertFalse(repositories().isEmpty(), "Must specify repositories");

    for (RepositoryConfig r : repositories.values()) {
      r.validate();
    }

    for (EditorConfig e : getEditorConfigs().values()) {
      e.validate();
    }

    for (TranslatorConfig t : getTranslators()) {
      t.validate();
    }

    for (MigrationConfig m : getMigrationConfigs()) {
      m.validate();
    }
  }

  public static ProjectConfig makeProjectConfigFromConfigText(String configText)
      throws InvalidProject {
    try {
      Gson gson = MoeModule.provideGson(); // TODO(user): Remove this static reference.
      ProjectConfig config = gson.fromJson(configText, ProjectConfig.class);
      if (config == null) {
        throw new InvalidProject("Could not parse MOE config");
      }

      config.validate();
      return config;
    } catch (JsonParseException e) {
      throw new InvalidProject("Could not parse MOE config: " + e.getMessage());
    }
  }
}
