// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.project;

import junit.framework.TestCase;

/**
 * @author dbentley@google.com (Daniel Bentley)
 */
public class ProjectConfigTest extends TestCase {

  public void testValidConfig() throws Exception {
    ProjectConfig p = ProjectConfig.makeProjectConfigFromConfigText(
        "{\"name\": \"foo\", \"repositories\": {\"public\": {}}}");
    assertEquals(p.getName(), "foo");
  }

  public void testInvalidConfig() throws Exception {
    assertInvalidConfig(
        "{}",
        "Must specify a name");
  }

  public void testInvalidConfig2() throws Exception {
    assertInvalidConfig(
        "{\"name\": \"foo\", \"repositories\": {}}",
        "Must specify repositories");
  }

  private void assertInvalidConfig(String text, String error) {
    try {
      ProjectConfig.makeProjectConfigFromConfigText(text);
      fail();
    } catch (InvalidProject e) {
      assertEquals(error, e.explanation);
    }
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
