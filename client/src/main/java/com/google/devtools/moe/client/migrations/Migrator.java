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

package com.google.devtools.moe.client.migrations;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.config.MigrationConfig;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher.Result;
import com.google.devtools.moe.client.InvalidProject;
import com.google.devtools.moe.client.config.ScrubberConfig;
import com.google.devtools.moe.client.repositories.MetadataScrubber;
import com.google.devtools.moe.client.config.MetadataScrubberConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Perform the one_migration and migrate directives
 */
public class Migrator {
  private final Db db;
  private final DraftRevision.Factory revisionFactory;
  private final Map<Integer, MetadataScrubber> metadataScrubbers;
  private final Ui ui;

  @Inject
  public Migrator(
      DraftRevision.Factory revisionFactory,
      Map<Integer, MetadataScrubber> metadataScrubbers,
      Ui ui,
      Db db) {
    this.revisionFactory = revisionFactory;
    this.metadataScrubbers = new TreeMap<>(metadataScrubbers);
    this.ui = ui;
    this.db = db;
  }

  /**
   * Perform a migration from a Migration object. Includes metadata scrubbing.
   *
   * <p>The DraftRevision is created at the revision of last equivalence, or from the head/tip
   * of the repository if no Equivalence could be found.
   *
   * @param migration the Migration representing the migration to perform
   * @param repositoryType the RepositoryType of the from repository.
   * @param destination the Writer to put the changes from the Migration into
   *
   * @return  a DraftRevision on success, or null on failure
   */
  public DraftRevision migrate(
      Migration migration,
      RepositoryType repositoryType,
      Codebase fromCodebase,
      Revision mostRecentFromRev,
      MetadataScrubberConfig metadataScrubberConfig,
      ScrubberConfig scrubberConfig,
      Writer destination) {

    RevisionHistory revisionHistory = repositoryType.revisionHistory();
    RevisionMetadata metadata =
        processMetadata(
            revisionHistory, migration.fromRevisions(), metadataScrubberConfig, mostRecentFromRev);

    return revisionFactory.create(
        fromCodebase, destination, possiblyScrubAuthors(metadata, scrubberConfig));
  }

  public RevisionMetadata possiblyScrubAuthors(RevisionMetadata metadata, ScrubberConfig scrubber) {
    try {
      if (scrubber != null && scrubber.shouldScrubAuthor(metadata.author())) {
        return metadata.toBuilder().author(null).build();
      }
    } catch (InvalidProject exception) {
      throw new MoeProblem("%s", exception.getMessage());
    }
    return metadata;
  }

  /**
   * @param migrationConfig  the migration specification
   * @return a list of pending Migrations since last {@link RepositoryEquivalence} per
   *     migrationConfig
   */
  public List<Migration> findMigrationsFromEquivalency(
      RepositoryType fromRepo, MigrationConfig migrationConfig) {

    Result equivMatch =
        matchEquivalences(fromRepo.revisionHistory(), migrationConfig.getToRepository());

    List<Revision> revisionsSinceEquivalence =
        Lists.reverse(equivMatch.getRevisionsSinceEquivalence().getBreadthFirstHistory());

    if (revisionsSinceEquivalence.isEmpty()) {
      ui.message(
          "No revisions found since last equivalence for migration '%s'",
          migrationConfig.getName());
      return ImmutableList.of();
    }

    List<RepositoryEquivalence> equivalences = equivMatch.getEquivalences();
    if (equivalences.isEmpty()) {
      // Occurs if Moe magic is used on an empty target repo.
      throw new IllegalStateException(
           "Cannot migrate to an empty repository.  "
           + "Follow the steps in \"Make the Initial Push.\"");
    }
    RepositoryEquivalence lastEq = equivalences.get(0);
    ui.message(
        "Found %d revisions in %s since equivalence (%s): %s",
        revisionsSinceEquivalence.size(),
        migrationConfig.getFromRepository(),
        lastEq,
        Joiner.on(", ").join(revisionsSinceEquivalence));

    if (migrationConfig.getSeparateRevisions()) {
      ImmutableList.Builder<Migration> migrations = ImmutableList.builder();
      for (Revision fromRev : revisionsSinceEquivalence) {
        migrations.add(
            Migration.create(
                migrationConfig.getName(),
                migrationConfig.getFromRepository(),
                migrationConfig.getToRepository(),
                ImmutableList.of(fromRev),
                lastEq));
      }
      return migrations.build();
    } else {
      return ImmutableList.of(
          Migration.create(
              migrationConfig.getName(),
              migrationConfig.getFromRepository(),
              migrationConfig.getToRepository(),
              revisionsSinceEquivalence,
              lastEq));
    }
  }

  /**
   * Returns a result containing the known/discovered equivalences.
   */
  public Result matchEquivalences(RevisionHistory history, String toRepository) {
    // TODO(cgruber): Migrate this to a graph walk.
    Result equivMatch =
        history.findRevisions(
            null, // Start at head.
            new RepositoryEquivalenceMatcher(toRepository, db),
            SearchType.LINEAR);
    return equivMatch;
  }

  /**
   * Get and scrub RevisionMetadata based on the given MetadataScrubberConfig.
   */
  public RevisionMetadata processMetadata(
      RevisionHistory revisionHistory,
      List<Revision> revs,
      @Nullable MetadataScrubberConfig sc,
      @Nullable Revision fromRevision) {
    ImmutableList.Builder<RevisionMetadata> rmBuilder = ImmutableList.builder();

    for (Revision rev : revs) {
      RevisionMetadata rm = revisionHistory.getMetadata(rev);
      for (MetadataScrubber scrubber : metadataScrubbers.values()) {
        try {
          rm = scrubber.scrub(rm, sc);
        } catch (RuntimeException e) {
          throw new MoeProblem(
              e, "Error processing %s: %s", scrubber.getClass().getSimpleName(), e.getMessage());
        }
      }
      rmBuilder.add(rm);
    }

    return RevisionMetadata.concatenate(rmBuilder.build(), fromRevision);
  }
}
