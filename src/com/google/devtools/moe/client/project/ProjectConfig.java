// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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
    if (repositories == null) {
      repositories = Maps.newHashMap();
    }

    // For backwards compatibility with old MOE configs.
    if (internalRepository != null) {
      if (repositories.put("internal", internalRepository) != null) {
        throw new InvalidProject("Internal repository specified twice");
      }
    }
    if (publicRepository != null) {
      if (repositories.put("public", publicRepository) != null) {
        throw new InvalidProject("Public repository specified twice");
      }
    }

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

  public static ProjectConfig makeProjectConfigFromConfigText(String configText)
      throws InvalidProject {
    try {
      Gson gson = makeGson();
      ProjectConfig config = gson.fromJson(configText, ProjectConfig.class);
      if (config == null) {
        throw new InvalidProject("Could not parse MOE config");
      } else if (config.getName() == null ||
          config.getName().isEmpty()) {
        throw new InvalidProject("Must specify a name");
      } else if (config.getRepositoryConfigs().isEmpty()) {
        throw new InvalidProject("Must specify repositories");
      }
      return config;
    } catch (JsonParseException e) {
      throw new InvalidProject("Could not parse MOE config: " + e.getMessage());
    }
  }
}
