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

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db.HasDbStorage;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.config.ProjectConfig;
import com.google.devtools.moe.client.qualifiers.Argument;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.testing.DummyDb;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dagger.Provides;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A file-backed implementation of MOE {@link Db}.
 */
public class FileDb implements Db, HasDbStorage {

  private final String location;
  private final DbStorage dbStorage;
  private final FileDb.Writer writer;

  // TODO(cgruber): Rationalize DbStorage.
  public FileDb(String location, DbStorage dbStorage, FileDb.Writer writer) {
    this.location = location;
    this.dbStorage = dbStorage;
    this.writer = writer;
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
  public boolean hasMigration(SubmittedMigration migration) {
    return dbStorage.hasMigration(migration);
  }

  @Override
  public DbStorage getStorage() {
    return dbStorage;
  }

  @Override
  public void write() {
    writer.write(this);
  }

  /**
   * Writes a database implementing {@link HasDbStorage} to the supplied filesystem at a given
   * location, or at the location originally attached to the database.
   */
  public static class Writer {
    private final Gson gson;
    private final FileSystem filesystem;

    @Inject
    public Writer(Gson gson, FileSystem filesystem) {
      this.gson = gson;
      this.filesystem = filesystem;
    }

    public void write(Db db) {
      if (db instanceof HasDbStorage) {
        try {
          DbStorage storage = ((HasDbStorage) db).getStorage();
          filesystem.write(gson.toJson(storage) + "\n", new File(db.location()));
        } catch (IOException e) {
          throw new MoeProblem(e, "I/O Error writing database");
        }
      } else {
        throw new MoeProblem("Database does not support exporting its internal storage.");
      }
    }
  }

  /** A Factory to produce {@link FileDb} instances. */
  static class Factory {
    private final Gson gson;
    private final FileSystem filesystem;

    @Inject
    Factory(FileSystem filesystem, Gson gson) {
      this.filesystem = filesystem;
      this.gson = gson;
    }

    FileDb load(Path location) throws MoeProblem, InvalidProject {
      try {
        if (filesystem.exists(location.toFile())) {
          String dbText = filesystem.fileToString(location.toFile());
          try {
            DbStorage dbStorage = gson.fromJson(dbText, DbStorage.class);
            return new FileDb(location.toString(), dbStorage, new FileDb.Writer(gson, filesystem));
          } catch (JsonParseException e) {
            throw new InvalidProject("Could not parse MOE DB: " + e.getMessage());
          }
        } else {
          return new FileDb(
              location.toString(), new DbStorage(), new FileDb.Writer(gson, filesystem));
        }
      } catch (IOException e) {
        throw new MoeProblem(e, "Could not load %s", location);
      }
    }
  }

  /** Supplies the various bindings needed to use this database in a dagger graph. */
  @dagger.Module
  public static class Module {

    // TODO(cgruber): Make this into a map-binding based dispatch so new db types can be added.
    @Provides
    @Singleton
    static Db db(
        ProjectConfig config,
        @Nullable @Argument("db") String override,
        @Argument("help") boolean helpFlag,
        FileDb.Factory factory,
        Ui ui) {
      if (helpFlag) {
        return new Db.NoopDb();
      }

      String location = !isNullOrEmpty(override) ? override : config.databaseUri();
      if (isNullOrEmpty(location)) {
        throw new InvalidProject(
            "Database location was not set in the project configuration nor on the command-line.");
      }
      if (location.equals("dummy") || location.startsWith("dummy:")) {
        return new DummyDb(true, ui);
      }

      // File-based
      Path path;
      if (location.startsWith("file:")) {
        try {
          path = Paths.get(new URI(location));
        } catch (URISyntaxException | IllegalArgumentException e) {
          throw new InvalidProject(e, "Invalid URI for database location: %s", location);
        }
      } else {
        // Legacy: assume a file db.
        path = Paths.get(location);
      }
      return factory.load(path);
    }
  }
}
