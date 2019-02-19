package com.google.devtools.moe.client.project;

import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.config.ProjectConfig;
import com.google.devtools.moe.client.GsonModule;
import com.google.devtools.moe.client.gson.JsonStructureChecker;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.json.JsonSanitizer;

import java.io.StringReader;

/** Produces a project config from a JSON string representation. */
public class ProjectConfigs {
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
          throw new InvalidProject(e, "Could not parse MOE config: " + e.getMessage());
        }
      }

      if (config == null) {
        throw new InvalidProject("Could not parse MOE config");
      }
      config.validate();
      return config;
    }
}
