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

package com.google.devtools.moe.client.config;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.InvalidProject;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Configuration for a MOE Project
 */
@AutoValue
public abstract class ProjectConfig {

  /** The name of this project */
  @Nullable
  public abstract String name();

  /**
   * Location of the database MOE should use for this project.
   *
   * <p>While this should be a URI, implementers may choose to honor "/foo" as "file:///foo" or
   * "foo" as a relative file to support legacy clients.
   *
   * <p>This can be overridden by the {@code --db} command-line flag, and can be null, in which case
   * any code-path that requires a database will require that it be set on the command-line.
   */
  @Nullable
  @SerializedName("database_uri") // TODO(cushon): remove pending rharter/auto-value-gson#18
  public abstract String databaseUri();

  /** The set of configured editors for this project */
  public abstract Map<String, EditorConfig> editors();

  /** The set of migration configurations that have been set for this project. */
  public abstract ImmutableList<MigrationConfig> migrations();

  /** The configured repositories this project is aware of. */
  public abstract Map<String, RepositoryConfig> repositories();

  /** The set of translations that have been configured for this project. */
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

    public abstract Builder databaseUri(String name);

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
          "No such repository '%s' in the config. Found: %s",
          repositoryName, ImmutableSortedSet.copyOf(repositories().keySet()));
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

  public void validate() throws InvalidProject {
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

  public static TypeAdapter<ProjectConfig> typeAdapter(Gson gson) {
    return new AutoValue_ProjectConfig.GsonTypeAdapter(gson);
  }
}
