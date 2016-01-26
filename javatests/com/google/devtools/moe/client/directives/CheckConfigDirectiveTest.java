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

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.dvcs.git.GitRepositoryFactory;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.svn.SvnRepositoryFactory;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

public class CheckConfigDirectiveTest extends TestCase {
  private final RecordingUi ui = new RecordingUi();
  private final SystemCommandRunner cmd = new SystemCommandRunner(ui);
  private final Repositories repositories =
      new Repositories(
          ImmutableSet.of(
              new DummyRepositoryFactory(null),
              new GitRepositoryFactory(cmd, null),
              new SvnRepositoryFactory(null, null)));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(null, cmd, null, ui, repositories);

  public void testEmptyConfigFilenameThrows() throws Exception {
    contextFactory.projectConfigs.put("moe_config.txt", "");
    CheckConfigDirective d = new CheckConfigDirective(contextFactory);
    try {
      d.perform();
      fail();
    } catch (InvalidProject expected) {
    }
  }

  public void testEmptyConfigFileReturnsOne() throws Exception {
    contextFactory.projectConfigs.put("moe_config.txt", "");
    CheckConfigDirective d = new CheckConfigDirective(contextFactory);
    d.setContextFileName("moe_config.txt");
    try {
      d.perform();
      fail();
    } catch (InvalidProject expected) {
    }
  }

  public void testSimpleConfigFileWorks() throws Exception {
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{\"name\": \"foo\", \"repositories\": {\"public\": {\"type\": \"dummy\"}}}");
    CheckConfigDirective d = new CheckConfigDirective(contextFactory);
    d.setContextFileName("moe_config.txt");
    assertEquals(0, d.perform());
  }
}
