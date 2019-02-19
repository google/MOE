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

import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.ExpressionEngine;
import com.google.devtools.moe.client.config.ProjectConfig;
import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.devtools.moe.client.config.TranslatorConfig;
import com.google.devtools.moe.client.config.UsernamesConfig;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.translation.editors.Editors;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Creates a ProjectContext from a configuration file loaded from the file system.
 */
public class FileReadingProjectContextFactory extends ProjectContextFactory {
  private final FileSystem fileSystem;
  private final Gson gson;

  @Inject
  public FileReadingProjectContextFactory(
      ExpressionEngine expressionEngine, Ui ui, Repositories repositories, Editors editors,
      FileSystem fileSystem, Gson gson) {
    super(expressionEngine, ui, repositories, editors);
    this.fileSystem = fileSystem;
    this.gson = gson;
  }

  @Override
  public ProjectConfig loadConfiguration(String configFilename) throws InvalidProject {
    String configText;
    try (Task t = ui.newTask("read_config", "Reading config file from %s", configFilename)) {
      configText = fileSystem.fileToString(new File(configFilename));
      return ProjectConfigs.parse(configText);
    } catch (IOException e) {
      throw new InvalidProject("Config File \"" + configFilename + "\" not accessible.");
    }
  }

  @Override
  public void loadUsernamesFiles(ProjectConfig config) {
    for (TranslatorConfig translatorConfig : config.translators()) {
      for (ScrubberConfig scrubberConfig : translatorConfig.scrubbers()) {
        if (scrubberConfig == null || scrubberConfig.getUsernamesFile() == null) {
          continue;
        }
        scrubberConfig.updateUsernames(parseUsernamesConfig(scrubberConfig.getUsernamesFile()));
      }
    }
  }

  private UsernamesConfig parseUsernamesConfig(String usernamesFilePath) {
    File usernamesFile = new File(usernamesFilePath);

    try (Task t =
        ui.newTask("read_usernames_file", "Reading usernames file from %s", usernamesFilePath)) {
      return gson.fromJson(fileSystem.fileToString(usernamesFile), UsernamesConfig.class);
    } catch (IOException exception) {
      throw new InvalidProject(
          "File " + usernamesFilePath + " referenced by usernames_file not found.");
    }
  }
}
