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

package com.google.devtools.moe.client.testing;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.database.SubmittedMigration;
import com.google.devtools.moe.client.repositories.Revision;

import java.util.ArrayList;
import java.util.Set;

/**
 * Db for testing
 */
public class DummyDb implements Db {

  private static final Joiner JOINER = Joiner.on("\n");

  private final Ui ui;
  private final boolean returnEquivalences;

  public ArrayList<RepositoryEquivalence> equivalences;
  public ArrayList<SubmittedMigration> migrations;


  public DummyDb(boolean returnEquivalences, Ui ui) {
    this.ui = ui;
    this.returnEquivalences = returnEquivalences;
    this.equivalences = new ArrayList<RepositoryEquivalence>();
    this.migrations = new ArrayList<SubmittedMigration>();
  }

  @Override
  public String location() {
    return "dummy://";
  }

  @Override
  public void noteEquivalence(RepositoryEquivalence equivalence) {
    equivalences.add(equivalence);
  }

  @Override
  public Set<Revision> findEquivalences(Revision revision, String otherRepository) {
    if (!returnEquivalences) {
      return ImmutableSet.of();
    } else {
      return ImmutableSet.of(
          Revision.create(1, otherRepository), Revision.create(2, otherRepository));
    }
  }

  @Override
  public boolean noteMigration(SubmittedMigration migration) {
    return !migrations.contains(migration) && migrations.add(migration);
  }

  @Override
  public boolean hasMigration(SubmittedMigration migration) {
    return migrations.contains(migration);
  }

  @Override
  public void write() {
    ui.message(
        "Equivalences:\n%s\nMigrations:\n%s",
        JOINER.join(this.equivalences),
        JOINER.join(this.migrations));
  }
}
