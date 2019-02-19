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

package com.google.devtools.moe.client.repositories;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.moe.client.testing.DummyRevisionHistory.parseLegacyFields;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.config.MetadataScrubberConfig;

import java.util.List;
import junit.framework.TestCase;
import org.joda.time.DateTime;

/**
 * Unit test for the MetadataUsernameScrubber.
 *
 */
public class MetadataUsernameScrubberTest extends TestCase {
  private final MetadataUsernameScrubber mus = new MetadataUsernameScrubber();

  public void testScrubOnOneUser() throws Exception {
    RevisionMetadata before =
        RevisionMetadata.builder()
            .id("100")
            .author("Bob Saget")
            .date(new DateTime(1L))
            .description("saget fixed ALL the bugs.")
            .build();
    before = parseLegacyFields(before);
    RevisionMetadata expected =
        RevisionMetadata.builder()
            .id("100")
            .author("Bob user")
            .date(new DateTime(1L))
            .description("user fixed ALL the bugs.")
            .build();
    expected = parseLegacyFields(expected);
    RevisionMetadata after =
        mus.scrub(
            before,
            new MetadataScrubberConfig() {
              @Override
              public List<String> getUsernamesToScrub() {
                return ImmutableList.of("saget");
              }
            });
    assertThat(after).isEqualTo(expected);
  }

  public void testScrubOnOneUserWithMailBrackets() throws Exception {
    RevisionMetadata before =
        RevisionMetadata.builder()
            .id("100")
            .author("<bob>")
            .date(new DateTime(1L))
            .description("bob fixed ALL the bugs.")
            .build();
    before = parseLegacyFields(before);
    RevisionMetadata expected =
        RevisionMetadata.builder()
            .id("100")
            .author("<user>")
            .date(new DateTime(1L))
            .description("user fixed ALL the bugs.")
            .build();
    expected = parseLegacyFields(expected);
    RevisionMetadata after =
        mus.scrub(
            before,
            new MetadataScrubberConfig() {
              @Override
              public List<String> getUsernamesToScrub() {
                return ImmutableList.of("bob");
              }
            });
    assertThat(after).isEqualTo(expected);
  }

  public void testScrubOnSimpleUsername() throws Exception {
    RevisionMetadata before =
        RevisionMetadata.builder()
            .id("100")
            .author("bob")
            .date(new DateTime(1L))
            .description("bob fixed ALL the bugs.")
            .build();
    before = parseLegacyFields(before);
    RevisionMetadata expected =
        RevisionMetadata.builder()
            .id("100")
            .author("user")
            .date(new DateTime(1L))
            .description("user fixed ALL the bugs.")
            .build();
    expected = parseLegacyFields(expected);
    RevisionMetadata after =
        mus.scrub(
            before,
            new MetadataScrubberConfig() {
              @Override
              public List<String> getUsernamesToScrub() {
                return ImmutableList.of("bob");
              }
            });
    assertThat(after).isEqualTo(expected);
  }

  public void testScrubOnTwoUsers() throws Exception {
    RevisionMetadata before =
        RevisionMetadata.builder()
            .id("100")
            .author("Bob, Saget")
            .date(new DateTime(1L))
            .description("bob and saget fixed ALL the bugs.")
            .build();
    before = parseLegacyFields(before);
    RevisionMetadata expected =
        RevisionMetadata.builder()
            .id("100")
            .author("user, user")
            .date(new DateTime(1L))
            .description("user and user fixed ALL the bugs.")
            .build();
    expected = parseLegacyFields(expected);

    RevisionMetadata after =
        mus.scrub(
            before,
            new MetadataScrubberConfig() {
              @Override
              public List<String> getUsernamesToScrub() {
                return ImmutableList.of("bob", "saget");
              }
            });
    assertThat(after).isEqualTo(expected);
  }

  public void testFalsePositive() throws Exception {
    RevisionMetadata before =
        RevisionMetadata.builder()
            .id("100")
            .author("Bob, Saget")
            .date(new DateTime(1L))
            .description("bob and \nsaget went bobbing for apples in Sagetopolis.")
            .build();
    before = parseLegacyFields(before);
    RevisionMetadata expected =
        RevisionMetadata.builder()
            .id("100")
            .author("user, user")
            .date(new DateTime(1L))
            .description("user and \nuser went bobbing for apples in Sagetopolis.")
            .build();
    expected = parseLegacyFields(expected);

    RevisionMetadata after =
        mus.scrub(
            before,
            new MetadataScrubberConfig() {
              @Override
              public List<String> getUsernamesToScrub() {
                return ImmutableList.of("bob", "saget");
              }
            });
    assertThat(after).isEqualTo(expected);
  }
}
