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
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.joda.time.DateTime;

public class DescriptionMetadataScrubberTest extends TestCase {

  private static final RevisionMetadata REVISION_METADATA =
      new RevisionMetadata(
          "commit_number",
          "author@google.com",
          new DateTime(1L),
          "some changes",
          ImmutableList.of(
              Revision.create("parentId1", "repo"), Revision.create("parentId2", "repo")));

  public void testNonDescriptionFieldsUnaffacted() {
    RevisionMetadata rmExpected =
        new RevisionMetadata(
            "commit_number",
            "author@google.com",
            new DateTime(1L),
            "some changes!!!",
            ImmutableList.of(
                Revision.create("parentId1", "repo"), Revision.create("parentId2", "repo")));

    RevisionMetadata rmActual =
        new DescriptionMetadataScrubber()
            .scrub(
                REVISION_METADATA,
                new MetadataScrubberConfig() {
                  @Override
                  public String getLogFormat() {
                    return "{description}!!!";
                  }
                });
    assertThat(rmActual).isEqualTo(rmExpected);
  }

  public void testVariousScrubbingFormats() {
    assertFormatResults("{description} on {date}", "some changes on 1969/12/31");
    assertFormatResults(
        "{description} on {date}\nby: {author}",
        "some changes on 1969/12/31\nby: author@google.com");
    assertFormatResults(
        "{date} saw the birth of {id}", "1969/12/31 saw the birth of commit_number");
    assertFormatResults("my parents are ({parents})", "my parents are (parentId1, parentId2)");
  }

  private void assertFormatResults(final String format, String expectedOutput) {
    MetadataScrubberConfig config =
        new MetadataScrubberConfig() {
          @Override
          public String getLogFormat() {
            return format;
          }
        };
    assertWithMessage("Unexpected scrubbing output for format '%s'", format)
        .that(new DescriptionMetadataScrubber().scrub(REVISION_METADATA, config).description)
        .isEqualTo(expectedOutput);
  }
}
