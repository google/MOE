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
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.DummyDb;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.tools.EagerLazy;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;

public class LastEquivalenceDirectiveTest extends TestCase {
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final Ui ui = new Ui(stream, /* fileSystem */ null);
  private final SystemCommandRunner cmd = new SystemCommandRunner();
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory(null)));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(null, cmd, null, ui, repositories);
  private final DummyDb.Factory dbFactory = new DummyDb.Factory(true);

  public void testPerform() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"internal\": {\"type\": \"dummy\"}}}");
    ProjectContext context = contextFactory.create("moe_config.txt");
    LastEquivalenceDirective d =
        new LastEquivalenceDirective(EagerLazy.fromInstance(context), dbFactory, ui);
    d.dbLocation = "dummy";
    d.fromRepository = "internal(revision=1)";
    d.withRepository = "public";
    assertThat(d.perform()).isEqualTo(0);
    assertThat(stream.toString()).contains("Last equivalence: internal{1} == public{1}");
  }
}