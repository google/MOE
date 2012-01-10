// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
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

  public Map<String, RepositoryConfig> getRepositoryConfigs()
      throws InvalidProject {
    Preconditions.checkNotNull(repositories);
    return Collections.unmodifiableMap(repositories);
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

  /**
   * Helper class to deserialize raw Json in a config.
   */
  static class JsonObjectDeserializer implements JsonDeserializer<JsonObject> {
    @Override
        public JsonObject deserialize(
            JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
      return json.getAsJsonObject();
    }
  }

  /**
   * Make a GSON object usable to parse MOE configs.
   */
  public static Gson makeGson() {
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(JsonObject.class, new JsonObjectDeserializer())
        .create();
    return gson;
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

    InvalidProject.assertFalse(
        Strings.isNullOrEmpty(getName()), "Must specify a name");
    InvalidProject.assertFalse(
        getRepositoryConfigs().isEmpty(), "Must specify repositories");

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
      Gson gson = makeGson();
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
