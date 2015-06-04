// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.database;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * A file-backed implementation of MOE {@link Db}.
 *
 */
public class FileDb implements Db {

  private static final Gson FILE_DB_GSON = new GsonBuilder().setPrettyPrinting().create();

  private final DbStorage dbStorage;

  public FileDb (DbStorage dbStorage) {
    this.dbStorage = dbStorage;
  }

  /**
   * @return all Equivalences stored in the database
   */
  public Set<Equivalence> getEquivalences() {
    return ImmutableSet.copyOf(dbStorage.getEquivalences());
  }

  @Override
  public void noteEquivalence(Equivalence equivalence) {
    dbStorage.addEquivalence(equivalence);
  }

  @Override
  public Set<Revision> findEquivalences(Revision revision, String otherRepository) {
    ImmutableSet.Builder<Revision> equivalentToRevision = ImmutableSet.builder();
    for (Equivalence e : dbStorage.getEquivalences()) {
      if (e.hasRevision(revision)) {
        Revision otherRevision = e.getOtherRevision(revision);
        if (otherRevision.repositoryName.equals(otherRepository)) {
          equivalentToRevision.add(otherRevision);
        }
      }
    }
    return equivalentToRevision.build();
  }

  @Override
  public boolean noteMigration(SubmittedMigration migration) {
    return dbStorage.addMigration(migration);
  }

  @VisibleForTesting
  public String toJsonString() {
    return FILE_DB_GSON.toJson(dbStorage) + "\n";
  }

  @Override
  public void writeToLocation(String dbLocation) {
    try {
      Injector.INSTANCE.fileSystem().write(toJsonString(), new File(dbLocation));
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }
  }

  public static FileDb makeDbFromDbText(String dbText) throws InvalidProject {
    try {
      DbStorage dbStorage = FILE_DB_GSON.fromJson(dbText, DbStorage.class);
      return new FileDb(dbStorage);
    } catch (JsonParseException e) {
      throw new InvalidProject("Could not parse MOE DB: " + e.getMessage());
    }
  }

  public static FileDb makeDbFromFile(String path) throws MoeProblem {
    try {
      String dbText = Injector.INSTANCE.fileSystem().fileToString(new File(path));
      try {
        return makeDbFromDbText(dbText);
      } catch (InvalidProject e) {
        throw new MoeProblem(e.getMessage());
      }
    } catch (IOException e) {
      throw new MoeProblem(e.getMessage());
    }
  }
}
