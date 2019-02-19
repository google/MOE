/*
 * Copyright (c) 2015 Google, Inc.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.devtools.moe.client.GsonModule;

import junit.framework.TestCase;

import java.lang.reflect.Field;

public class RepositoryConfigTest extends TestCase {

  @SuppressWarnings("unchecked")
  public void testIgnoreFilePatternsUsingDeprecatedAndNewMethod() throws Exception {
    String json =
        Joiner.on('\n')
            .join(
                "{",
                "  'project_space': 'public',",
                "  'type': 'dummy',",
                "  'ignore_file_patterns':[",
                "    '.*foo'",
                "  ],",
                "  'ignore_file_res':[",
                "    '.*bar'",
                "  ]",
                "}")
            .replace('\'', '\n');
    RepositoryConfig config = GsonModule.provideGson().fromJson(json, RepositoryConfig.class);
    assertThat((Iterable<String>) get(config, "ignoreFilePatterns")).isNotEmpty();
    assertThat((Iterable<String>) get(config, "ignoreFileRes")).isNotEmpty();
    try {
      config.getIgnoreFilePatterns();
      fail("Should throw.");
    } catch (InvalidProject expected) {
    }
  }

  @SuppressWarnings("unchecked")
  public void testExecutableFilePatternsUsingDeprecatedAndNewMethod() throws Exception {
    String json =
        Joiner.on('\n')
            .join(
                "{",
                "  'project_space': 'public',",
                "  'type': 'dummy',",
                "  'executable_file_patterns':[",
                "    '.*foo'",
                "  ],",
                "  'executable_file_res':[",
                "    '.*bar'",
                "  ]",
                "}")
            .replace('\'', '\n');
    RepositoryConfig config = GsonModule.provideGson().fromJson(json, RepositoryConfig.class);
    assertThat((Iterable<String>) get(config, "executableFilePatterns")).isNotEmpty();
    assertThat((Iterable<String>) get(config, "executableFileRes")).isNotEmpty();
    try {
      config.getExecutableFilePatterns();
      fail("Should throw.");
    } catch (InvalidProject expected) {
    }
  }

  private Object get(Object instance, String property) {
    try {
      Field field = instance.getClass().getDeclaredField(property);
      field.setAccessible(true);
      return field.get(instance);
    } catch (
        NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
            e) {
      throw new RuntimeException(e);
    }
  }
}
