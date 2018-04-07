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
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RevisionMetadataTest {
  private static final RevisionMetadata DEFAULT_TEST_METADATA =
      RevisionMetadata.builder()
          .id("id")
          .author("auth")
          .date(new DateTime(1L))
          .description("description")
          .build();

  @Test
  public void testConcatenate_singleMetadata() {
    RevisionMetadata rm =
        DEFAULT_TEST_METADATA.toBuilder().withParents(Revision.create("revId", "repo")).build();

    assertThat(RevisionMetadata.concatenate(ImmutableList.of(rm), null)).isEqualTo(rm);
  }

  @Test
  public void testLegacyParseFields() {
    RevisionMetadata rm =
        DEFAULT_TEST_METADATA
            .toBuilder()
            .description("Foo\n" + "FOO_BAR=blah\n" + "foo-bar=blah=foo")
            .build();
    rm = parseLegacyFields(rm);

    assertThat(rm.fields()).containsEntry("FOO_BAR", "blah");
    assertThat(rm.fields()).containsEntry("foo-bar", "blah=foo");
    assertThat(rm.fields()).hasSize(2);
  }

  @Test
  public void testConcatenate_twoMetadata() {
    RevisionMetadata rm1 =
        RevisionMetadata.builder()
            .id("id1")
            .author("auth1")
            .date(new DateTime(1L))
            .description("description1")
            .withParents(Revision.create("revId1", "repo"))
            .build();
    RevisionMetadata rm2 =
        RevisionMetadata.builder()
            .id("id2")
            .author("auth2")
            .date(new DateTime(2L))
            .description("description2")
            .withParents(Revision.create("revId2", "repo"))
            .build();

    RevisionMetadata rmExpected =
        RevisionMetadata.builder()
            .id("id1, id2")
            .author("auth1, auth2")
            .date(new DateTime(2L))
            .description("description1\n\n-------------\ndescription2")
            .withParents(Revision.create("revId1", "repo"), Revision.create("revId2", "repo"))
            .build();
    rmExpected = parseLegacyFields(rmExpected);

    assertThat(RevisionMetadata.concatenate(ImmutableList.of(rm1, rm2), null))
        .isEqualTo(rmExpected);
  }

  @Test
  public void testConcatenate_withMigrationInfo() {
    RevisionMetadata rm1 =
        RevisionMetadata.builder()
            .id("id1")
            .author("auth1")
            .date(new DateTime(1L))
            .description("description1")
            .withParents(Revision.create("revId1", "repo"))
            .build();
    RevisionMetadata rm2 =
        RevisionMetadata.builder()
            .id("id2")
            .author("auth2")
            .date(new DateTime(2L))
            .description("description2")
            .withParents(Revision.create("revId2", "repo"))
            .build();

    Revision migrationFromRev = Revision.create("migrationRevId", "repo");

    RevisionMetadata rmExpected =
        RevisionMetadata.builder()
            .id("id1, id2")
            .author("auth1, auth2")
            .date(new DateTime(2L))
            .description(
                "description1\n\n-------------\ndescription2"
                    + "\n\n-------------\nCreated by MOE: https://github.com/google/moe\n"
                    + "MOE_MIGRATED_REVID="
                    + migrationFromRev.revId())
            .withParents(Revision.create("revId1", "repo"), Revision.create("revId2", "repo"))
            .build();
    rmExpected = parseLegacyFields(rmExpected);

    assertThat(RevisionMetadata.concatenate(ImmutableList.of(rm1, rm2), migrationFromRev))
        .isEqualTo(rmExpected);
  }
}
