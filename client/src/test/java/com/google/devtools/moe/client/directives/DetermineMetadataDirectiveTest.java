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

package com.google.devtools.moe.client.directives;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.MetadataScrubber;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.writer.DraftRevision;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;
import org.joda.time.DateTime;

public class DetermineMetadataDirectiveTest extends TestCase {
  private static final ImmutableMap<Integer, MetadataScrubber> NO_SCRUBBERS = ImmutableMap.of();
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final Ui ui = new Ui(stream, /* fileSystem */ null);
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory()));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(null, ui, repositories);

  /**
   *  When two or more revisions are given, the metadata fields are concatenated.
   */
  public void testDetermineMetadata() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    ProjectContext context = contextFactory.create("moe_config.txt");
    DetermineMetadataDirective d =
        new DetermineMetadataDirective(
            context, ui, new Migrator(new DraftRevision.Factory(ui), NO_SCRUBBERS, ui, null));
    d.repositoryExpression = "internal(revision=\"1,2\")";
    assertEquals(0, d.perform());
    RevisionMetadata expected =
        RevisionMetadata.builder()
            .id("1, 2")
            .author("author, author")
            .date(new DateTime(1L))
            .description("description\n\n-------------\ndescription")
            .withParents(
                Revision.create("parent", "internal"), Revision.create("parent", "internal"))
            .build();

    assertThat(stream.toString().trim()).isEqualTo(expected.toString().trim());
  }

  /**
   *  When only one revision is given, the new metadata should be identical to
   *  that revision's metadata.
   */
  public void testDetermineMetadataOneRevision() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    ProjectContext context = contextFactory.create("moe_config.txt");
    DetermineMetadataDirective d =
        new DetermineMetadataDirective(
            context, ui, new Migrator(new DraftRevision.Factory(ui), NO_SCRUBBERS, ui, null));
    d.repositoryExpression = "internal(revision=7)";
    assertEquals(0, d.perform());
    RevisionMetadata rm =
        RevisionMetadata.builder()
            .id("7")
            .author("author")
            .date(new DateTime(1L))
            .description("description")
            .withParents(Revision.create("parent", "internal"))
            .build();

    assertThat(stream.toString().trim()).isEqualTo(rm.toString().trim());
  }
}