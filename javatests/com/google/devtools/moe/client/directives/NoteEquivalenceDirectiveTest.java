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

import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.FileDb;
import com.google.devtools.moe.client.gson.GsonModule;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.testing.DummyRepositoryFactory;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class NoteEquivalenceDirectiveTest extends TestCase {
  private final ByteArrayOutputStream stream = new ByteArrayOutputStream();
  private final Ui ui = new Ui(stream, /* fileSystem */ null);
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFs = control.createMock(FileSystem.class);
  private final SystemCommandRunner cmd = new SystemCommandRunner();
  private final Repositories repositories =
      new Repositories(ImmutableSet.<RepositoryType.Factory>of(new DummyRepositoryFactory(mockFs)));
  private final InMemoryProjectContextFactory contextFactory =
      new InMemoryProjectContextFactory(null, cmd, mockFs, ui, repositories);
  private final Db.Factory dbFactory = new FileDb.Factory(mockFs, GsonModule.provideGson());
  private final Db.Writer dbWriter = new FileDb.Writer(GsonModule.provideGson(), mockFs);

  NoteEquivalenceDirective d;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    contextFactory.projectConfigs.put(
        "moe_config.txt",
        "{'name': 'foo', 'repositories': {"
            + "  'internal': {'type': 'dummy'}, 'public': {'type': 'dummy'}"
            + "}}");
    ProjectContext context = contextFactory.create("moe_config.txt");
    super.setUp();
    // TODO(cgruber): Rip this out when Db.Factory is injected.
    Injector.INSTANCE = new Injector(mockFs, cmd, ui);

    d = new NoteEquivalenceDirective(context, dbFactory, dbWriter, ui);
    d.dbLocation = "/foo/db.txt";
  }

  public void testPerform_invalidRepo() throws Exception {
    d.repo1 = "nonexistent(revision=2)";
    d.repo2 = "public(revision=3)";

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(false);

    control.replay();
    try {
      d.perform();
      fail("NoteEquivalenceDirective didn't fail on invalid repository 'nonexistent'.");
    } catch (MoeProblem expected) {
      assertEquals(
          "No such repository 'nonexistent' in the config. Found: [internal, public]",
          expected.getMessage());
    }
    control.verify();
  }

  public void testPerform_newDbFile() throws Exception {
    d.repo1 = "internal(revision=1)";
    d.repo2 = "public(revision=4)";

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(false);
    mockFs.write(
        Joiner.on('\n')
            .join(
                "{",
                "  'equivalences': [",
                "    {",
                "      'rev1': {",
                "        'repository_name': 'internal',",
                "        'rev_id': '1'",
                "      },",
                "      'rev2': {",
                "        'repository_name': 'public',",
                "        'rev_id': '4'",
                "      }",
                "    }",
                "  ],",
                "  'migrations': []",
                "}")
            .replace('\'', '"'),
        new File("/foo/db.txt"));

    control.replay();
    int result = d.perform();
    control.verify();

    assertEquals(0, result);
  }

  public void testPerform_existingDbFile_noChanges() throws Exception {
    d.repo1 = "internal(revision=1)";
    d.repo2 = "public(revision=4)";

    String dbString =
        Joiner.on('\n')
            .join(
                "{",
                "  'equivalences': [",
                "    {",
                "      'rev1': {",
                "        'repository_name': 'internal',",
                "        'rev_id': '1'",
                "      },",
                "      'rev2': {",
                "        'repository_name': 'public',",
                "        'rev_id': '4'",
                "      }",
                "    }",
                "  ],",
                "  'migrations': []",
                "}")
            .replace('\'', '"');

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(true);
    expect(mockFs.fileToString(new File("/foo/db.txt"))).andReturn(dbString);
    mockFs.write(dbString, new File("/foo/db.txt"));

    control.replay();
    int result = d.perform();
    control.verify();

    assertEquals(0, result);
  }

  public void testPerform_existingDbFile_addEquivalence() throws Exception {
    d.repo1 = "internal(revision=1)";
    d.repo2 = "public(revision=4)";

    String baseDbString =
        Joiner.on('\n')
            .join(
                "{",
                "  'equivalences': [",
                "    {",
                "      'rev1': {",
                "        'repository_name': 'internal',",
                "        'rev_id': '0'",
                "      },",
                "      'rev2': {",
                "        'repository_name': 'public',",
                "        'rev_id': '3'",
                "      }",
                "    }%s", // New equivalence is added here.
                "  ],",
                "  'migrations': []",
                "}")
            .replace('\'', '"');

    String oldDbString = String.format(baseDbString, "");
    String newDbString =
        String.format(
                baseDbString,
                Joiner.on('\n')
                    .join(
                        ",",
                        "    {",
                        "      'rev1': {",
                        "        'repository_name': 'internal',",
                        "        'rev_id': '1'",
                        "      },",
                        "      'rev2': {",
                        "        'repository_name': 'public',",
                        "        'rev_id': '4'",
                        "      }",
                        "    }"))
            .replace('\'', '"');

    expect(mockFs.exists(new File("/foo/db.txt"))).andReturn(true);
    expect(mockFs.fileToString(new File("/foo/db.txt"))).andReturn(oldDbString);
    mockFs.write(newDbString, new File("/foo/db.txt"));

    control.replay();
    int result = d.perform();
    control.verify();

    assertEquals(0, result);
  }
}