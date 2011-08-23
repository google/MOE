// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.AppContextForTesting;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;

import java.io.File;

/**
 */
public class FileDbTest extends TestCase {

  @Override
  public void setUp() {
    AppContextForTesting.initForTest();
  }

  public void testValidDb() throws Exception {
    FileDb db = new FileDb(FileDb.makeDbFromDbText(
        "{\"equivalences\":[{\"rev1\":" +
        "{\"revId\":\"r1\",\"repositoryName\":\"name1\"},\"rev2\":" +
        "{\"revId\":\"r2\",\"repositoryName\":\"name2\"}}]}"));
    assertEquals(db.getEquivalences(), ImmutableSet.of(
        new Equivalence(new Revision("r1", "name1"), new Revision("r2", "name2"))));
  }

  public void testInvalidDb() throws Exception {
    try {
      FileDb.makeDbFromDbText("{}");
      fail("Expected InvalidProject exception with empty db text");
    } catch (InvalidProject e) {}
  }

  public void testNoteEquivalence() throws Exception {
    FileDb db = new FileDb(FileDb.makeDbFromDbText("{\"equivalences\":[]}"));
    Equivalence e = new Equivalence(new Revision("r1", "name1"), new Revision("r2", "name2"));
    db.noteEquivalence(e);
    assertEquals(db.getEquivalences(), ImmutableSet.of(e));
  }

  public void testFindEquivalences() throws Exception {
    FileDb db = new FileDb(FileDb.makeDbFromDbText(
        "{\"equivalences\":[{\"rev1\":" +
        "{\"revId\":\"r1\",\"repositoryName\":\"name1\"},\"rev2\":" +
        "{\"revId\":\"r2\",\"repositoryName\":\"name2\"}}," +
        "{\"rev1\":" +
        "{\"revId\":\"r3\",\"repositoryName\":\"name2\"},\"rev2\":" +
        "{\"revId\":\"r1\",\"repositoryName\":\"name1\"}}]}"));
    assertEquals(db.findEquivalences(new Revision("r1", "name1"), "name2"),
                 ImmutableSet.of(new Revision("r2", "name2"), new Revision("r3", "name2")));
  }

  public void testMakeDbFromFile() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    AppContext.RUN.fileSystem = fileSystem;

    File dbFile = new File("/path/to/db");
    String dbText =
        "{\"equivalences\":[{\"rev1\":" +
        "{\"revId\":\"r1\",\"repositoryName\":\"name1\"},\"rev2\":" +
        "{\"revId\":\"r2\",\"repositoryName\":\"name2\"}}]}";
    expect(fileSystem.fileToString(dbFile)).andReturn(dbText);
    control.replay();
    FileDb db = new FileDb(FileDb.makeDbFromDbText(dbText));
    assertEquals((new FileDb(FileDb.makeDbFromFile(dbFile.getPath()))).getEquivalences(),
                 db.getEquivalences());
    control.verify();
  }

  public void testWriteDbToFile() throws Exception {
    IMocksControl control = EasyMock.createControl();
    FileSystem fileSystem = control.createMock(FileSystem.class);
    AppContext.RUN.fileSystem = fileSystem;

    File dbFile = new File("/path/to/db");
    String dbText =
        "{\"equivalences\":[{\"rev1\":" +
        "{\"revId\":\"r1\",\"repositoryName\":\"name1\"},\"rev2\":" +
        "{\"revId\":\"r2\",\"repositoryName\":\"name2\"}}]}";
    fileSystem.write(dbText, dbFile);
    control.replay();
    FileDb db = new FileDb(FileDb.makeDbFromDbText(dbText));
    FileDb.writeDbToFile(db, dbFile.getPath());
    control.verify();
  }
}
