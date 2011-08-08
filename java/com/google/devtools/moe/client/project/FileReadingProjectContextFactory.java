// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.Ui;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class FileReadingProjectContextFactory implements ProjectContextFactory {

  public ProjectContext makeProjectContext(String configFilename) throws InvalidProject{
    String configText;
    Ui.Task task = AppContext.RUN.ui.pushTask(
        "read_config",
        String.format("Reading config file from %s", configFilename));
    try {
      try {
        configText = Files.toString(new File(configFilename), Charsets.UTF_8);
      } catch (IOException e) {
        throw new InvalidProject(
            "Config File \"" + configFilename + "\" not accessible.");
      }

      return ProjectContext.makeProjectContextFromConfigText(configText);
    } finally {
      AppContext.RUN.ui.popTask(task, "");
    }
  }

}
