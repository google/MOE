// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Access to MOE's database when kept as a file
 *
 */
public class FileDb implements Db {

  private final DbStorage db;

  public FileDb (DbStorage db) {
    this.db = db;
  }

  /**
   * @return all Equivalences stored in the database
   */
  public Set<Equivalence> getEquivalences() {
    return ImmutableSet.copyOf(db.equivalences);
  }

  /**
   * @param equivalence  the Equivalence to add to the database
   */
  public void noteEquivalence(Equivalence equivalence) {
    db.equivalences.add(equivalence);
  }

  /**
   *  @param revision  the Revision to find equivalent revisions for. Two Revisions are equivalent
   *                   when there is an Equivalence containing them in the database.
   *  @param otherRepository  the Repository to find equivalent revisions in
   *
   *  @return all Revisions in otherRepository (have repositoryName of otherRepository) in some
   *          Equivalence with revision, or an empty set if none.
   */
  public Set<Revision> findEquivalences(Revision revision, String otherRepository) {
    Set<Revision> equivalentToRevision = Sets.newHashSet();
    for (Equivalence e : db.equivalences) {
      if (e.hasRevision(revision)) {
        Revision otherRevision = e.getOtherRevision(revision);
        if (otherRevision.repositoryName.equals(otherRepository)) {
          equivalentToRevision.add(otherRevision);
        }
      }
    }
    return ImmutableSet.copyOf(equivalentToRevision);
  }

  public static DbStorage makeDbFromDbText (String dbText)
      throws InvalidProject {
    try {
      Gson gson = new Gson();
      DbStorage db = gson.fromJson(dbText, DbStorage.class);
      if (db == null) {
        throw new InvalidProject("Could not parse MOE DB");
      } else if (db.equivalences == null) {
        throw new InvalidProject("MOE DB is missing equivalences");
      }
      return db;
    } catch (JsonParseException e) {
      throw new InvalidProject("Could not parse MOE DB: " + e.getMessage());
    }
  }

  public static DbStorage makeDbFromFile(String path) throws MoeProblem {
    try {
      String dbText = Files.toString(new File(path), Charsets.UTF_8);
      try {
        return makeDbFromDbText(dbText);
      } catch (InvalidProject e) {
        throw new MoeProblem(e.getMessage());
      }
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }
  }

  public static void writeDbToFile(FileDb d, String path) throws MoeProblem {
    try {
      File dbFile = new File(path);
      Gson gson = new Gson();
      String dbText = gson.toJson(d.db);
      Files.write(dbText, dbFile, Charsets.UTF_8);
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }
  }
}
