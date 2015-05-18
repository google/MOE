// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Files;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.repositories.Repositories;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

/**
 * Creates a ProjectContext from a configuration file loaded from the file system.
 */
public class FileReadingProjectContextFactory extends ProjectContextFactory {
  private final Ui ui;

  @Inject public FileReadingProjectContextFactory(Ui ui, Repositories repositories) {
    super(repositories);
    this.ui = ui;
  }

  @Override public ProjectConfig loadConfiguration(String configFilename) throws InvalidProject {
    String configText;
    Ui.Task task = ui.pushTask(
        "read_config",
        String.format("Reading config file from %s", configFilename));
    try {
      try {
        configText = Files.toString(new File(configFilename), UTF_8);
      } catch (IOException e) {
        throw new InvalidProject(
            "Config File \"" + configFilename + "\" not accessible.");
      }
      return ProjectConfig.makeProjectConfigFromConfigText(configText);
    } finally {
      ui.popTask(task, "");
    }
  }

}
