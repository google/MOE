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

import junit.framework.TestCase;

public class ProjectConfigTest extends TestCase {

  public void testValidConfig() throws Exception {
    ProjectConfig p =
        ProjectConfig.makeProjectConfigFromConfigText(
            "{\"name\": \"foo\", \"repositories\": {\"public\": {}}}");
    assertEquals(p.getName(), "foo");
  }

  public void testInvalidConfig() throws Exception {
    assertInvalidConfig("{}", "Must specify a name");
  }

  public void testInvalidConfig2() throws Exception {
    assertInvalidConfig("{\"name\": \"foo\", \"repositories\": {}}", "Must specify repositories");
  }

  public void testInvalidEditor() {
    assertInvalidConfig(
        "{"
            + " 'name': 'foo',"
            + " 'repositories': {"
            + "   'x': {}"
            + " },"
            + " 'editors': {'e1': {}}"
            + "}",
        "Missing type in editor");
  }

  public void testInvalidTranslators1() {
    assertInvalidConfig(
        "{"
            + " 'name': 'foo',"
            + " 'repositories': {"
            + "   'x': {}"
            + " },"
            + " 'translators': [{}]"
            + "}",
        "Translator requires from_project_space");
  }

  public void testInvalidTranslators2() {
    assertInvalidConfig(
        "{"
            + " 'name': 'foo',"
            + " 'repositories': {"
            + "   'x': {}"
            + " },"
            + " 'translators': [{'from_project_space': 'x'}]"
            + "}",
        "Translator requires to_project_space");
  }

  public void testInvalidTranslators3() {
    assertInvalidConfig(
        "{"
            + " 'name': 'foo',"
            + " 'repositories': {"
            + "   'x': {}"
            + " },"
            + " 'translators': [{'from_project_space': 'x',"
            + "                  'to_project_space': 'y'}]"
            + "}",
        "Translator requires steps");
  }

  public void testInvalidStep1() {
    assertInvalidConfig(
        "{"
            + " 'name': 'foo',"
            + " 'repositories': {"
            + "   'x': {}"
            + " },"
            + " 'translators': [{'from_project_space': 'x',"
            + "                  'to_project_space': 'y',"
            + "                  'steps': [{'name': ''}]}]"
            + "}",
        "Missing name in step");
  }

  public void testInvalidStep2() {
    assertInvalidConfig(
        "{"
            + " 'name': 'foo',"
            + " 'repositories': {"
            + "   'x': {}"
            + " },"
            + " 'translators': [{'from_project_space': 'x',"
            + "                  'to_project_space': 'y',"
            + "                  'steps': [{'name': 'x'}]}]"
            + "}",
        "Missing editor in step");
  }

  public void testMigration1() {
    assertInvalidConfig(
        "{"
            + " 'name': 'foo',"
            + " 'repositories': {"
            + "   'x': {}"
            + " },"
            + " 'migrations': [{}]"
            + "}",
        "Missing name in migration");
  }

  private void assertInvalidConfig(String text, String error) {
    try {
      ProjectConfig.makeProjectConfigFromConfigText(text);
      fail("Expected error");
    } catch (InvalidProject e) {
      assertEquals(error, e.getMessage());
    }
  }

  public void testConfigWithMultipleRepositories() throws Exception {
    assertInvalidConfig(
        "{\"name\": \"foo\","
            + "\"repositories\": {"
            + "\"internal\": {\"type\":\"svn\"},"
            + "\"internal\": {\"type\":\"svn\"}}}",
        "Could not parse MOE config: duplicate key: internal");
  }

  public void testConfigWithScrubberConfig() throws Exception {
    // The scrubber config should not be parsed.
    ProjectConfig p =
        ProjectConfig.makeProjectConfigFromConfigText(
            "{\"name\": \"foo\","
                + " \"scrubber_config\": {\"a\": 1, \"b\": 2},"
                + " \"repositories\": {\"internal\": {\"type\":\"svn\"}}"
                + "}");
    assertEquals(1, p.repositories().size());
    assertNotNull(p.getRepositoryConfig("internal"));
  }
}
