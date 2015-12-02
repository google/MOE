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

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.database.Db.HasDbStorage;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.DummyDb;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import dagger.Provides;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A file-backed implementation of MOE {@link Db}.
 */
public class FileDb implements Db, HasDbStorage {

  private final String location;
  private final DbStorage dbStorage;

  // TODO(cgruber): Rationalize DbStorage.
  public FileDb(String location, DbStorage dbStorage) {
    this.location = location;
    this.dbStorage = dbStorage;
  }

  @Override
  public String location() {
    return location;
  }

  /**
   * @return all Equivalences stored in the database
   */
  public Set<RepositoryEquivalence> getEquivalences() {
    return ImmutableSet.copyOf(dbStorage.equivalences());
  }

  @Override
  public void noteEquivalence(RepositoryEquivalence equivalence) {
    dbStorage.addEquivalence(equivalence);
  }

  @Override
  public Set<Revision> findEquivalences(Revision revision, String otherRepository) {
    ImmutableSet.Builder<Revision> equivalentToRevision = ImmutableSet.builder();
    for (RepositoryEquivalence e : dbStorage.equivalences()) {
      if (e.hasRevision(revision)) {
        Revision otherRevision = e.getOtherRevision(revision);
        if (otherRevision.repositoryName().equals(otherRepository)) {
          equivalentToRevision.add(otherRevision);
        }
      }
    }
    return equivalentToRevision.build();
  }

  /**
   * @return all {@link SubmittedMigration} objects stored in the database
   */
  public Set<SubmittedMigration> getMigrations() {
    return ImmutableSet.copyOf(dbStorage.migrations());
  }

  @Override
  public boolean noteMigration(SubmittedMigration migration) {
    return dbStorage.addMigration(migration);
  }

  @Override
  public DbStorage getStorage() {
    return dbStorage;
  }

  /**
   * Writes a database implementing {@link HasDbStorage} to the supplied filesystem at a given
   * location, or at the location originally attached to the database.
   */
  public static class Writer implements Db.Writer {
    private final Gson gson;
    private final FileSystem filesystem;

    @Inject
    public Writer(Gson gson, FileSystem filesystem) {
      this.gson = gson;
      this.filesystem = filesystem;
    }

    @Override
    public void write(Db db) {
      writeToLocation(db.location(), db);
    }

    @Override
    public void writeToLocation(String dbLocation, Db db) {
      if (db instanceof HasDbStorage) {
        try {
          DbStorage storage = ((HasDbStorage) db).getStorage();
          filesystem.write(gson.toJson(storage), new File(dbLocation));
        } catch (IOException e) {
          throw new MoeProblem("I/O Error writing database: " + e.getMessage());
        }
      } else {
        throw new MoeProblem("Database does not support exporting its internal storage.");
      }
    }
  }

  /** An injectable Factory to produce {@link FileDb} instances. */
  // TODO(cgruber) @AutoFactory?
  public static class Factory implements Db.Factory {
    private final Gson gson;
    private final FileSystem filesystem;

    @Inject
    public Factory(FileSystem filesystem, Gson gson) {
      this.filesystem = filesystem;
      this.gson = gson;
    }

    @Override
    public Db parseJson(String dbText) throws InvalidProject {
      return parseJson(null, dbText);
    }

    public Db parseJson(String location, String dbText) throws InvalidProject {
      try {
        DbStorage dbStorage = gson.fromJson(dbText, DbStorage.class);
        return new FileDb(location, dbStorage);
      } catch (JsonParseException e) {
        throw new InvalidProject("Could not parse MOE DB: " + e.getMessage());
      }
    }

    @Override
    public Db load(String location) throws MoeProblem {
      if (location.equals("dummy")) {
        return new DummyDb(true);
      } else {
        try {
          if (filesystem.exists(new File(location))) {
            String dbText = filesystem.fileToString(new File(location));
            return parseJson(location, dbText);
          } else {
            return new FileDb(location, new DbStorage());
          }
        } catch (IOException e) {
          throw new MoeProblem(e.getMessage());
        }
      }
    }
  }

  /** Supplies the various bindings needed to use this database in a dagger graph. */
  @dagger.Module
  public static class Module {
    @Provides
    @Singleton
    Db.Factory dbFactory(FileDb.Factory impl) {
      return impl;
    }

    @Provides
    @Singleton
    Db.Writer dbWriter(FileDb.Writer impl) {
      return impl;
    }
  }
}
