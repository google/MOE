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
package com.google.devtools.moe.client.options;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

/**
 * Tests for {@link OptionsModule}
 */
public class OptionsModuleTest extends TestCase {
  public void testConfigFileOptionParsing() {
    assertThat(OptionsModule.configFile("-c", "foo/bar")).isEqualTo("foo/bar");
    assertThat(OptionsModule.configFile("--config", "foo/bar")).isEqualTo("foo/bar");
    assertThat(OptionsModule.configFile("--config_file", "foo/bar")).isEqualTo("foo/bar");
    assertThat(OptionsModule.configFile("-c=foo/bar")).isEqualTo("foo/bar");
    assertThat(OptionsModule.configFile("--config=foo/bar")).isEqualTo("foo/bar");
    assertThat(OptionsModule.configFile("--config_file=foo/bar")).isEqualTo("foo/bar");
  }

  public void testConfigFileOptionParsing_noConfig() {
    assertThat(OptionsModule.configFile("a", "b")).isNull();
  }

  public void testConfigFileOptionParsing_configMissingFromParameterList() {
    expectException("-c");
    expectException("--config");
    expectException("--config_file");
  }

  public void testConfigFileOptionParsing_configFollowedByFlag() {
    expectException("-c", "-f");
    expectException("--config", "-f");
    expectException("--config_file", "-f");
  }

  private static void expectException(String... args) {
    try {
      OptionsModule.configFile(args);
      fail("Expected failure with args: " + ImmutableList.copyOf(args));
    } catch (IllegalArgumentException expected) {
    }
  }
}
