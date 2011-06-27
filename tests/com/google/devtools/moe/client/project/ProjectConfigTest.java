// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ProjectConfigTest extends TestCase {

  public void testValidConfig() throws Exception {
    ProjectConfig p = ProjectConfig.makeProjectConfigFromConfigText(
        "{\"name\": \"foo\", \"repositories\": {}}");
    assertEquals(p.getName(), "foo");
  }

  public void testInvalidConfig() throws Exception {
    try {
      ProjectConfig.makeProjectConfigFromConfigText("{}");
      fail();
    } catch (InvalidProject e) {}
  }

  public void testConfigWithMultipleRepositories() throws Exception {
    ProjectConfig p = ProjectConfig.makeProjectConfigFromConfigText(
        "{\"name\": \"foo\"," +
        "\"repositories\": {"+
        "\"internal\": {\"type\":\"svn\"}," +
        "\"internal\": {\"type\":\"svn\"}}}");
    assertEquals(1, p.getRepositoryConfigs().size());
  }

  public void testConfigWithScrubberConfig() throws Exception {
    // The scrubber config should not be parsed.
    ProjectConfig p = ProjectConfig.makeProjectConfigFromConfigText(
        "{\"name\": \"foo\"," +
        " \"scrubber_config\": {\"a\": 1, \"b\": 2}," +
        " \"repositories\": {\"internal\": {\"type\":\"svn\"}}" +
        "}");
    assertEquals(1, p.getRepositoryConfigs().size());
  }

}
