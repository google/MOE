// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ProjectContextFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class InMemoryProjectContextFactory implements ProjectContextFactory {

  public Map<String, String> projectConfigs;

  public InMemoryProjectContextFactory() {
    projectConfigs = new HashMap<String, String>();
  }

  public ProjectContext makeProjectContext(String configFilename) throws InvalidProject {
    return ProjectContext.makeProjectContextFromConfigText(projectConfigs.get(configFilename));
  }

}
