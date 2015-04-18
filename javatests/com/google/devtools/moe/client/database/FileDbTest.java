// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.Moe;
import com.google.devtools.moe.client.SystemCommandRunner;
import com.google.devtools.moe.client.SystemFileSystem;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.InMemoryProjectContextFactory;
import com.google.devtools.moe.client.testing.RecordingUi;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

import javax.inject.Singleton;

/**
 */
public class FileDbTest extends TestCase {
  // TODO(cgruber): Rework these when statics aren't inherent in the design.
  @dagger.Component(modules = {
      RecordingUi.Module.class,
      InMemoryProjectContextFactory.Module.class,
      SystemCommandRunner.Module.class,
      SystemFileSystem.Module.class})
  @Singleton
  interface Component extends Moe.Component {
    @Override Injector context(); // TODO (b/19676630) Remove when bug is fixed.
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    Injector.INSTANCE = DaggerFileDbTest_Component.create().context();
  }

  public void testValidDb() throws Exception {
    String dbText = Joiner.on("\n").join(
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
        "}");
    FileDb db = FileDb.makeDbFromDbText(dbText);
    assertEquals(db.getEquivalences(), ImmutableSet.of(
        new Equivalence(new Revision("r1", "name1"), new Revision("r2", "name2"))));
  }

  public void testEmptyDb() throws Exception {
    FileDb db = FileDb.makeDbFromDbText("{}");
    assertTrue(db.getEquivalences().isEmpty());
  }

  public void testNoteEquivalence() throws Exception {
    FileDb db = FileDb.makeDbFromDbText("{\"equivalences\":[]}");
    Equivalence e = new Equivalence(new Revision("r1", "name1"), new Revision("r2", "name2"));
    db.noteEquivalence(e);
    assertEquals(db.getEquivalences(), ImmutableSet.of(e));
  }

  public void testNoteMigration() throws Exception {
    FileDb db = FileDb.makeDbFromDbText("{}");
    SubmittedMigration migration = new SubmittedMigration(
        new Revision("r1", "name1"), new Revision("r2", "name2"));
    assertTrue(db.noteMigration(migration));
    // The migration has already been added, so noting it again should return false.
    assertFalse(db.noteMigration(migration));
  }

  public void testFindEquivalences() throws Exception {
    String dbText = Joiner.on("\n").join(
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
        "}");

    FileDb db = FileDb.makeDbFromDbText(dbText);
    assertEquals(db.findEquivalences(new Revision("r1", "name1"), "name2"),
                 ImmutableSet.of(new Revision("r2", "name2"), new Revision("r3", "name2")));
  }

  public void testMakeDbFromFile() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    Injector.INSTANCE.fileSystem = fileSystem;

    File dbFile = new File("/path/to/db");
    String dbText = Joiner.on("\n").join(
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
        "}");

    expect(fileSystem.fileToString(dbFile)).andReturn(dbText);

    control.replay();
    assertEquals(
        FileDb.makeDbFromDbText(dbText).getEquivalences(),
        FileDb.makeDbFromFile(dbFile.getPath()).getEquivalences());
    control.verify();
  }

  public void testWriteDbToFile() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    Injector.INSTANCE.fileSystem = fileSystem;

    File dbFile = new File("/path/to/db");
    String dbText = Joiner.on("\n").join(
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
        "  'migrations': []",
        "}");

    fileSystem.write(dbText.replace('\'', '"'), dbFile);

    control.replay();
    FileDb.makeDbFromDbText(dbText).writeToLocation(dbFile.getPath());
    control.verify();
  }
}
