// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.devtools.moe.client.Ui;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class FileReadingProjectContextFactory implements ProjectContextFactory {

  @Inject Ui ui;

  public ProjectContext makeProjectContext(String configFilename) throws InvalidProject{
    String configText;
    Ui.Task task = ui.pushTask(
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
      ui.popTask(task, "");
    }
  }

}
