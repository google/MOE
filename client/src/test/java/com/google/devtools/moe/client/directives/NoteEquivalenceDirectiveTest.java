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
import static org.easymock.EasyMock.expectLastCall;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.DbStorage;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class NoteEquivalenceDirectiveTest extends TestCase {
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final Ui ui = new Ui(stream, /* fileSystem */ null);
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory()));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(null, ui, repositories);
  private final IMocksControl control = EasyMock.createControl();
  private final FileDb.Writer dbWriter = control.createMock(FileDb.Writer.class);
  private final DbStorage dbStorage = new DbStorage();
  private final Db db = new FileDb("/foo/db.txt", dbStorage, dbWriter);

  private NoteEquivalenceDirective d;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{'name': 'foo', 'database_file': '/foo/db.txt', 'repositories': {"
            + "  'internal': {'type': 'dummy'}, 'public': {'type': 'dummy'}"
            + "}}");
    ProjectContext context = contextFactory.create("moe_config.txt");
    super.setUp();

    d = new NoteEquivalenceDirective(context, db, ui);
  }

  public void testPerform_invalidRepo() throws Exception {
    d.repo1 = "nonexistent(revision=2)";
    d.repo2 = "public(revision=3)";

    try {
      d.perform();
      fail("NoteEquivalenceDirective didn't fail on invalid repository 'nonexistent'.");
    } catch (MoeProblem expected) {
      assertEquals(
          "No such repository 'nonexistent' in the config. Found: [internal, public]",
          expected.getMessage());
    }
  }

  public void testPerform_noChanges() throws Exception {
    d.repo1 = "internal(revision=1)";
    d.repo2 = "public(revision=4)";

    dbWriter.write(db);
    expectLastCall();

    control.replay();
    assertThat(d.perform()).isEqualTo(0);
    control.verify();

    DbStorage expectedStorage = new DbStorage();
    expectedStorage.addEquivalence(
        RepositoryEquivalence.create(Revision.create(1, "internal"), Revision.create(4, "public")));
    assertThat(dbStorage).isEqualTo(expectedStorage);
  }

  public void testPerform_existingDbFile_addEquivalence() throws Exception {
    d.repo1 = "internal(revision=1)";
    d.repo2 = "public(revision=4)";

    dbStorage.addEquivalence(
        RepositoryEquivalence.create(Revision.create(0, "internal"), Revision.create(3, "public")));

    dbWriter.write(db);
    expectLastCall();

    control.replay();
    assertThat(d.perform()).isEqualTo(0);
    control.verify();

    DbStorage expectedStorage = new DbStorage();
    expectedStorage.addEquivalence(
        RepositoryEquivalence.create(Revision.create(0, "internal"), Revision.create(3, "public")));
    expectedStorage.addEquivalence(
        RepositoryEquivalence.create(Revision.create(1, "internal"), Revision.create(4, "public")));
    assertThat(dbStorage).isEqualTo(expectedStorage);
  }
}