// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

/**
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public interface ProjectContextFactory {

  /**
   * Make a ProjectContext for this config filename.
   *
   * @param configFilename the name of the holding the config
   *
   * @return the ProjectContext to be used
   */
  public ProjectContext makeProjectContext(String configFilename) throws InvalidProject;
}
