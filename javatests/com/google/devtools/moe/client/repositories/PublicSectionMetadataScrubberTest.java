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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.joda.time.DateTime;

/**
 * Tests for PublicSectionMetadataScrubber.
 *
 */
public class PublicSectionMetadataScrubberTest extends TestCase {

  private static RevisionMetadata makeWithDescription(String... desc) {
    return new RevisionMetadata(
        "commit_number",
        "author@google.com",
        new DateTime(1L),
        Joiner.on("\n").join(desc),
        ImmutableList.of(
            Revision.create("parentId1", "repo"), Revision.create("parentId2", "repo")));
  }

  public void testScrub() {
    RevisionMetadata rm =
        makeWithDescription(
            "Top secret stuff.",
            "",
            "Public:  ",
            "some changes",
            "intended for public use",
            "",
            "unrelated footer");

    // Test that fields besides description are unaffected.
    RevisionMetadata rmExpected = makeWithDescription("some changes", "intended for public use");
    assertEquals(rmExpected, new PublicSectionMetadataScrubber().scrub(rm));

    // Test various Strings.
    assertEquals(
        Joiner.on("\n").join("whitespace", "okay"),
        new PublicSectionMetadataScrubber()
            .scrub(makeWithDescription("Internal", "  Public: \t", "whitespace", "okay"))
            .description);

    assertEquals(
        Joiner.on("\n").join("no", "change"),
        new PublicSectionMetadataScrubber().scrub(makeWithDescription("no", "change")).description);

    assertEquals(
        "",
        new PublicSectionMetadataScrubber()
            .scrub(makeWithDescription("Internal", "Public:")).description);

    assertEquals(
        Joiner.on("\n").join("section", "first"),
        new PublicSectionMetadataScrubber()
            .scrub(makeWithDescription("Public:", "section", "first", "", "Then internal desc"))
            .description);

    assertEquals(
        "then public",
        new PublicSectionMetadataScrubber()
            .scrub(makeWithDescription("Unrelated:", "section", "Public:", "then public"))
            .description);

    assertEquals(
        "last public section",
        new PublicSectionMetadataScrubber()
            .scrub(
                makeWithDescription(
                    "Public:", "first public section", "", "Public:", "last public section"))
            .description);
  }
}
