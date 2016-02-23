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

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.DummyDb;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.tools.EagerLazy;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;

public class FindEquivalenceDirectiveTest extends TestCase {
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final Ui ui = new Ui(stream, /* fileSystem */ null);
  private final SystemCommandRunner cmd = new SystemCommandRunner();
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory(null)));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(null, cmd, null, ui, repositories);
  private final Db.Factory dbFactory = new DummyDb.Factory(true);

  public void testFindEquivalenceDirective() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"test\",\"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    ProjectContext context = contextFactory.create("moe_config.txt");
    FindEquivalenceDirective d =
        new FindEquivalenceDirective(EagerLazy.fromInstance(context), dbFactory, ui);
    d.dbLocation = "dummy";
    d.fromRepository = "internal(revision=1)";
    d.inRepository = "public";
    assertEquals(0, d.perform());
    assertThat(stream.toString()).contains("\"internal{1}\" == \"public{1,2}\"");
  }
}