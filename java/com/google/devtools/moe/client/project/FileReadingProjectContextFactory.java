// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.io.Files;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.tools.FileDifference.FileDiffer;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

/**
 * Creates a ProjectContext from a configuration file loaded from the file system.
 */
public class FileReadingProjectContextFactory extends ProjectContextFactory {

  @Inject
  public FileReadingProjectContextFactory(
      FileDiffer differ,
      CommandRunner cmd,
      FileSystem filesystem,
      Ui ui,
      Repositories repositories) {
    super(differ, cmd, filesystem, ui, repositories);
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
      return ProjectConfig.makeProjectConfigFromConfigText(configText);
    } finally {
      ui.popTask(task, "");
    }
  }
}