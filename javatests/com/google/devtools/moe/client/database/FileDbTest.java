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

package com.google.devtools.moe.client.database;

import static com.google.common.truth.Truth.assertThat;
import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.SystemFileSystem;
import com.google.devtools.moe.client.gson.GsonModule;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.gson.Gson;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Tests for the FileDb
 */
public class FileDbTest extends TestCase {
  private static final Gson GSON = GsonModule.provideGson();
  private final FileSystem filesystem = new SystemFileSystem();

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  public void testValidDb() throws Exception {
    String dbText =
        Joiner.on("\n")
            .join(
                "{",
                "  'equivalences': [",
                "    {",
                "      'rev1': {",
                "        'revId': 'r1',",
                "        'repositoryName': 'name1'",
                "      },",
                "      'rev2': {",
                "        'revId': 'r2',",
                "        'repositoryName': 'name2'",
                "      }",
                "    }",
                "  ]",
                "}",
                "");
    FileDb db = parseJson(dbText);
    assertEquals(
        db.getEquivalences(),
        ImmutableSet.of(
            RepositoryEquivalence.create(
                Revision.create("r1", "name1"), Revision.create("r2", "name2"))));
  }

  public void testEmptyDb() throws Exception {
    FileDb db = parseJson("{}");
    assertThat(db.getEquivalences()).isEmpty();
  }

  public void testNoteEquivalence() throws Exception {
    FileDb db = parseJson("{\"equivalences\":[]}");
    RepositoryEquivalence e =
        RepositoryEquivalence.create(
            Revision.create("r1", "name1"), Revision.create("r2", "name2"));
    db.noteEquivalence(e);
    assertEquals(db.getEquivalences(), ImmutableSet.of(e));
  }

  public void testNoteMigration() throws Exception {
    FileDb db = parseJson("{}");
    SubmittedMigration migration =
        SubmittedMigration.create(Revision.create("r1", "name1"), Revision.create("r2", "name2"));
    assertTrue(db.noteMigration(migration));
    // The migration has already been added, so noting it again should return false.
    assertFalse(db.noteMigration(migration));
  }

  public void testFindEquivalences() throws Exception {
    String dbText =
        Joiner.on("\n")
            .join(
                "{",
                "  'equivalences': [",
                "    {",
                "      'rev1': {",
                "        'revId': 'r1',",
                "        'repositoryName': 'name1'",
                "      },",
                "      'rev2': {",
                "        'revId': 'r2',",
                "        'repositoryName': 'name2'",
                "      }",
                "    },",
                "    {",
                "      'rev1': {",
                "        'revId': 'r3',",
                "        'repositoryName': 'name2'",
                "      },",
                "      'rev2': {",
                "        'revId': 'r1',",
                "        'repositoryName': 'name1'",
                "      }",
                "    }",
                "  ]",
                "}",
                "");

    FileDb db = parseJson(null, dbText);
    assertEquals(
        db.findEquivalences(Revision.create("r1", "name1"), "name2"),
        ImmutableSet.of(Revision.create("r2", "name2"), Revision.create("r3", "name2")));
  }

  public void testMakeDbFromFile() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem filesystem = control.createMock(FileSystem.class);
    File dbFile = new File("/path/to/db");
    FileDb.Factory factory = new FileDb.Factory(filesystem, GsonModule.provideGson());
    String dbText =
        Joiner.on("\n")
            .join(
                "{",
                "  'equivalences': [",
                "    {",
                "      'rev1': {",
                "        'revId': 'r1',",
                "        'repositoryName': 'name1'",
                "      },",
                "      'rev2': {",
                "        'revId': 'r2',",
                "        'repositoryName': 'name2'",
                "      }",
                "    }",
                "  ]",
                "}",
                "");

    expect(filesystem.fileToString(dbFile)).andReturn(dbText);
    expect(filesystem.exists(dbFile)).andReturn(true);

    control.replay();
    assertThat(factory.load(dbFile.toPath()).getEquivalences())
        .isEqualTo(parseJson(dbFile.getPath(), dbText).getEquivalences());
    control.verify();
  }

  public void testWriteDbToFile() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem filesystem = control.createMock(FileSystem.class);
    File dbFile = new File("/path/to/db");
    String dbText = "{\n  \"equivalences\": [],\n  \"migrations\": []\n}";
    DbStorage dbStorage = GSON.fromJson(dbText, DbStorage.class);
    Db db = new FileDb(dbFile.getPath(), dbStorage, new FileDb.Writer(GSON, filesystem));
    filesystem.write(dbText, dbFile);
    EasyMock.expectLastCall();
    control.replay();
    db.write();
    control.verify();
  }


  public void testSerialization() throws InvalidProject {
    String dbText =
        Joiner.on("\n")
            .join(
                "{",
                "  'equivalences': [",
                "    {",
                "      'rev1': {",
                "        'revId': 'r1',",
                "        'repositoryName': 'name1'",
                "      },",
                "      'rev2': {",
                "        'revId': 'r2',",
                "        'repositoryName': 'name2'",
                "      }",
                "    }",
                "  ],",
                "  'migrations': [",
                "    {",
                "      'fromRevision': {",
                "        'revId': 'r1',",
                "        'repositoryName': 'name1'",
                "      },",
                "      'toRevision': {",
                "        'revId': 'r2',",
                "        'repositoryName': 'name2'",
                "      }",
                "    }",
                "  ]",
                "}",
                "");
    FileDb db = parseJson(dbText.replace('\'', '"'));
    RepositoryEquivalence equivalence = Iterables.getOnlyElement(db.getEquivalences());
    assertEquals("r1", equivalence.getRevisionForRepository("name1").revId());
    assertEquals("r2", equivalence.getRevisionForRepository("name2").revId());
    SubmittedMigration migration = Iterables.getOnlyElement(db.getMigrations());
    assertEquals("name1", migration.fromRevision().repositoryName());
    assertEquals("r1", migration.fromRevision().revId());
    assertEquals("name2", migration.toRevision().repositoryName());
    assertEquals("r2", migration.toRevision().revId());
  }

  private FileDb parseJson(String dbText) throws InvalidProject {
    return parseJson(null, dbText);
  }

  private FileDb parseJson(String location, String dbText) throws InvalidProject {
    DbStorage dbStorage = GSON.fromJson(dbText, DbStorage.class);
    return new FileDb(location, dbStorage, new FileDb.Writer(GSON, filesystem));
  }

}
