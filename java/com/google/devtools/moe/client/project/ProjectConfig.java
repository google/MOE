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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.gson.AutoValueGsonAdapter;
import com.google.devtools.moe.client.gson.GsonModule;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.json.JsonSanitizer;

import java.io.StringReader;
import java.util.Map;

/**
 * Configuration for a MOE Project
 */
@AutoValue
@JsonAdapter(AutoValueGsonAdapter.class)
public abstract class ProjectConfig {

  public abstract String name();

  public abstract Map<String, EditorConfig> editors();

  public abstract ImmutableList<MigrationConfig> migrations();

  public abstract Map<String, RepositoryConfig> repositories();

  public abstract ImmutableList<TranslatorConfig> translators();

  public static Builder builder() {
    Builder builder = new AutoValue_ProjectConfig.Builder();
    builder.editors(ImmutableMap.<String, EditorConfig>of()); // default empty list.
    builder.migrations(ImmutableList.<MigrationConfig>of()); // default empty list.
    builder.repositories(ImmutableMap.<String, RepositoryConfig>of()); // default empty list.
    builder.translators(ImmutableList.<TranslatorConfig>of()); // default empty list.
    return builder;
  }

  /**
   * A standard builder pattern object to create a ProjectConfig.
   */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String name);

    public abstract Builder editors(Map<String, EditorConfig> editors);

    public abstract Builder migrations(ImmutableList<MigrationConfig> migrations);

    public abstract Builder repositories(Map<String, RepositoryConfig> repositories);

    public abstract Builder translators(ImmutableList<TranslatorConfig> translators);

    public abstract ProjectConfig build();
  }

  /**
   * Returns the {@link RepositoryConfig} in this config with the given name.
   *
   * @throws MoeProblem if no such repository with the given name exists
   */
  public RepositoryConfig getRepositoryConfig(String repositoryName) {
    if (!repositories().containsKey(repositoryName)) {
      throw new MoeProblem(
          "No such repository '"
              + repositoryName
              + "' in the config. Found: "
              + ImmutableSortedSet.copyOf(repositories().keySet()));
    }
    return repositories().get(repositoryName);
  }

  /**
   * Returns a configuration from one repository to another, if any is configured.
   */
  public TranslatorConfig findTranslatorFrom(String fromRepository, String toRepository) {
    String fromProjectSpace = getRepositoryConfig(fromRepository).getProjectSpace();
    String toProjectSpace = getRepositoryConfig(toRepository).getProjectSpace();
    for (TranslatorConfig translator : translators()) {
      if (translator.getFromProjectSpace().equals(fromProjectSpace)
          && translator.getToProjectSpace().equals(toProjectSpace)) {
        return translator;
      }
    }
    return null;
  }

  public ScrubberConfig findScrubberConfig(String fromRepository, String toRepository) {
    TranslatorConfig translator = findTranslatorFrom(fromRepository, toRepository);
    return (translator == null) ? null : translator.scrubber();
  }

  void validate() throws InvalidProject {
    InvalidProject.assertFalse(Strings.isNullOrEmpty(name()), "Must specify a name");
    InvalidProject.assertFalse(repositories().isEmpty(), "Must specify repositories");

    for (RepositoryConfig r : repositories().values()) {
      r.validate();
    }
    for (EditorConfig e : editors().values()) {
      e.validate();
    }
    for (TranslatorConfig t : translators()) {
      t.validate();
    }
    for (MigrationConfig m : migrations()) {
      m.validate();
    }
  }

  public static ProjectConfig parse(String configText) throws InvalidProject {
    ProjectConfig config = null;
    if (configText != null) {
      try {
        JsonReader configReader = new JsonReader(new StringReader(configText));
        configReader.setLenient(true);
        JsonElement configJsonElement = new JsonParser().parse(configReader);
        if (configJsonElement != null) {
          // Config files often contain JavaScript idioms like comments, single quoted strings,
          // and trailing commas in lists.
          // Check that the JSON parsed from configText is structurally the same as that
          // produced when it is interpreted by GSON in lax mode.
          String normalConfigText = JsonSanitizer.sanitize(configText);
          JsonElement normalConfigJsonElement = new JsonParser().parse(normalConfigText);
          JsonStructureChecker.requireSimilar(configJsonElement, normalConfigJsonElement);

          Gson gson = GsonModule.provideGson(); // TODO(user): Remove this static reference.
          config = gson.fromJson(configText, ProjectConfig.class);
        }
      } catch (JsonParseException e) {
        throw new InvalidProject("Could not parse MOE config: " + e.getMessage());
      }
    }

    if (config == null) {
      throw new InvalidProject("Could not parse MOE config");
    }
    config.validate();
    return config;
  }

}
