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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Files;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.translation.editors.Editors;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Creates a ProjectContext from a configuration file loaded from the file system.
 */
public class FileReadingProjectContextFactory extends ProjectContextFactory {

  @Inject
  public FileReadingProjectContextFactory(Ui ui, Repositories repositories, Editors editors) {
    super(ui, repositories, editors);
  }

  @Override
  public ProjectConfig loadConfiguration(String configFilename) throws InvalidProject {
    String configText;
    Ui.Task task = ui.pushTask("read_config", "Reading config file from %s", configFilename);
    try {
      try {
        configText = Files.toString(new File(configFilename), UTF_8);
      } catch (IOException e) {
        throw new InvalidProject("Config File \"" + configFilename + "\" not accessible.");
      }
      return ProjectConfig.parse(configText);
    } finally {
      ui.popTask(task, "");
    }
  }
}