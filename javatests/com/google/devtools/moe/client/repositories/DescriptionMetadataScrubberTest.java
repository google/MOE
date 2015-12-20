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

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.joda.time.DateTime;

public class DescriptionMetadataScrubberTest extends TestCase {

  public void testScrub() {
    RevisionMetadata rm =
        new RevisionMetadata(
            "commit_number",
            "author@google.com",
            new DateTime(1L),
            "some changes",
            ImmutableList.of(
                Revision.create("parentId1", "repo"), Revision.create("parentId2", "repo")));

    // Test that fields besides description are unaffected.
    RevisionMetadata rmExpected =
        new RevisionMetadata(
            "commit_number",
            "author@google.com",
            new DateTime(1L),
            "some changes!!!",
            ImmutableList.of(
                Revision.create("parentId1", "repo"), Revision.create("parentId2", "repo")));
    assertEquals(rmExpected, new DescriptionMetadataScrubber("{description}!!!").scrub(rm));

    // Test various formats.
    assertEquals(
        "some changes on 1970/01/01",
        new DescriptionMetadataScrubber("{description} on {date}").scrub(rm).description);

    assertEquals(
        "some changes on 1970/01/01\nby: author@google.com",
        new DescriptionMetadataScrubber("{description} on {date}\nby: {author}")
            .scrub(rm).description);

    assertEquals(
        "1970/01/01 saw the birth of commit_number",
        new DescriptionMetadataScrubber("{date} saw the birth of {id}").scrub(rm).description);

    assertEquals(
        "my parents are (parentId1, parentId2)",
        new DescriptionMetadataScrubber("my parents are ({parents})").scrub(rm).description);
  }
}
