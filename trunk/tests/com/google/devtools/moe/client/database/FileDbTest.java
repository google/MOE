// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.repositories.Revision;

import java.io.File;
import junit.framework.TestCase;

/**
 */
public class FileDbTest extends TestCase {

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
    File tempDir = Files.createTempDir();
    File dbFile = new File(tempDir.getPath() + "/db");
    Files.touch(dbFile);
    String dbText =
        "{\"equivalences\":[{\"rev1\":" +
        "{\"revId\":\"r1\",\"repositoryName\":\"name1\"},\"rev2\":" +
        "{\"revId\":\"r2\",\"repositoryName\":\"name2\"}}]}";
    Files.write(dbText, dbFile, Charsets.UTF_8);
    FileDb db = new FileDb(FileDb.makeDbFromDbText(dbText));
    assertEquals((new FileDb(FileDb.makeDbFromFile(dbFile.getPath()))).getEquivalences(),
                 db.getEquivalences());
  }

  public void testWriteDbToFile() throws Exception {
    File tempDir = Files.createTempDir();
    File dbWrittenFile = new File(tempDir.getPath() + "/dbWrite");
    Files.touch(dbWrittenFile);
    String dbText =
        "{\"equivalences\":[{\"rev1\":" +
        "{\"revId\":\"r1\",\"repositoryName\":\"name1\"},\"rev2\":" +
        "{\"revId\":\"r2\",\"repositoryName\":\"name2\"}}]}";
    FileDb db = new FileDb(FileDb.makeDbFromDbText(dbText));
    FileDb.writeDbToFile(db, dbWrittenFile.getPath());
    assertEquals(Files.toString(dbWrittenFile, Charsets.UTF_8), dbText);
  }
}
