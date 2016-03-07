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

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.joda.time.DateTime;

import java.util.List;

/**
 * Unit test for the MetadataUsernameScrubber.
 *
 */
public class MetadataUsernameScrubberTest extends TestCase {
  private final MetadataUsernameScrubber mus = new MetadataUsernameScrubber();

  public void testScrubOnOneUser() throws Exception {
    RevisionMetadata before =
        new RevisionMetadata(
            "100", "Bob Saget", new DateTime(1L), "saget fixed ALL the bugs.", null);
    RevisionMetadata expected =
        new RevisionMetadata(
            "100", "Bob <user>", new DateTime(1L), "<user> fixed ALL the bugs.", null);
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

  public void testScrubOnTwoUsers() throws Exception {
    RevisionMetadata before =
        new RevisionMetadata(
            "100", "Bob, Saget", new DateTime(1L), "bob and saget fixed ALL the bugs.", null);
    RevisionMetadata expected =
        new RevisionMetadata(
            "100",
            "<user>, <user>",
            new DateTime(1L),
            "<user> and <user> fixed ALL the bugs.",
            null);
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
        new RevisionMetadata(
            "100",
            "Bob, Saget",
            new DateTime(1L),
            "bob and \nsaget went bobbing for apples in Sagetopolis.",
            null);
    RevisionMetadata expected =
        new RevisionMetadata(
            "100",
            "<user>, <user>",
            new DateTime(1L),
            "<user> and \n<user> went bobbing for apples in Sagetopolis.",
            null);
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
