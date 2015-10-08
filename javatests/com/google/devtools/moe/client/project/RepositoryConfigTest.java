// Copyright 2015 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.project;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.devtools.moe.client.MoeModule;

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
    RepositoryConfig config = MoeModule.provideGson().fromJson(json, RepositoryConfig.class);
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
    RepositoryConfig config = MoeModule.provideGson().fromJson(json, RepositoryConfig.class);
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
