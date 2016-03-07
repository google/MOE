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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.joda.time.DateTime;

public class PublicSectionMetadataScrubberTest extends TestCase {

  private static final RevisionMetadata REVISION_METADATA =
      metadata(
          "Top secret stuff.",
          "",
          "Public:  ",
          "some changes",
          "intended for public use",
          "",
          "unrelated footer");

  private static final PublicSectionMetadataScrubber SCRUBBER = new PublicSectionMetadataScrubber();

  public void testNonDescriptionMetadataUnaffected() {
    RevisionMetadata rmExpected = metadata("some changes", "intended for public use");
    assertThat(SCRUBBER.scrub(REVISION_METADATA, null)).isEqualTo(rmExpected);
  }

  public void testPublicMessageReplacement() {
    String actual =
        SCRUBBER.scrub(metadata("Internal", "  Public: \t", "whitespace", "okay"), null)
            .description;
    String expected = Joiner.on("\n").join("whitespace", "okay");
    assertThat(actual).isEqualTo(expected);
  }

  public void testPublicMessageReplacement_EmptySection() {
    String actual = SCRUBBER.scrub(metadata("Internal", "Public:"), null).description;
    assertThat(actual).isEmpty();
  }

  public void testNoChange() {
    String actual = SCRUBBER.scrub(metadata("no", "change"), null).description;
    String expected = Joiner.on("\n").join("no", "change");
    assertThat(actual).isEqualTo(expected);
  }

  public void testPublicSectionFirst() {
    String actual =
        SCRUBBER.scrub(metadata("Public:", "section", "first", "", "Then internal desc"), null)
            .description;
    String expected = Joiner.on("\n").join("section", "first");
    assertThat(actual).isEqualTo(expected);
  }

  public void testUnrelatedSectionFirst() {
    String actual =
        SCRUBBER.scrub(metadata("Unrelated:", "section", "Public:", "then", "public"), null)
            .description;
    String expected = Joiner.on("\n").join("then", "public");
    assertThat(actual).isEqualTo(expected);
  }

  public void testSelectLastPublicSectionFromMoreThanOne() {
    String actual =
        SCRUBBER.scrub(
                metadata(
                    "Public:", "first", "public", "section", "", "Public:", "last", "public",
                    "section"),
                null)
            .description;
    String expected = Joiner.on("\n").join("last", "public", "section");
    assertThat(actual).isEqualTo(expected);
  }

  private static RevisionMetadata metadata(String... description) {
    return new RevisionMetadata(
        "commit_number",
        "author@google.com",
        new DateTime(1L),
        Joiner.on("\n").join(description),
        ImmutableList.of(
            Revision.create("parentId1", "repo"), Revision.create("parentId2", "repo")));
  }
}
