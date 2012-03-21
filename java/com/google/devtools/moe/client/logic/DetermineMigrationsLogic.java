// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;

import java.util.List;

/**
 * Given a {@link MigrationConfig} from Repo A to Repo B, return a List of {@link Migration}s
 * encapsulating all the changes from A that haven't been ported to B yet since their last
 * {@link Equivalence}.
 *
 */
public class DetermineMigrationsLogic {

  /**
   * @param migrationConfig  the migration specification
   * @param db  the MOE db which will be used to find the last equivalence
   * @return a list of pending Migrations since last {@link Equivalence} per migrationConfig
   */
  public static List<Migration> determineMigrations(
      ProjectContext context, MigrationConfig migrationConfig, Db db) {

    List<Revision> revisionsSinceEquivalence =
        RevisionsSinceEquivalenceLogic.getRevisionsSinceEquivalence(
            migrationConfig.getFromRepository(),
            migrationConfig.getToRepository(),
            db,
            context);

    if (revisionsSinceEquivalence.isEmpty()) {
      AppContext.RUN.ui.info("No revisions found since last equivalence for migration '"
          + migrationConfig.getName() + "'");
      return ImmutableList.of();
    }

    // History search goes backward from head, so reverse to get revisions in order.
    revisionsSinceEquivalence = Lists.reverse(revisionsSinceEquivalence);

    Equivalence lastEq = LastEquivalenceLogic.lastEquivalence(
        migrationConfig.getToRepository(),
        revisionsSinceEquivalence.get(revisionsSinceEquivalence.size() - 1),
        db,
        context.repositories.get(migrationConfig.getFromRepository()).revisionHistory);

    AppContext.RUN.ui.info(String.format("Found %d Revisions since equivalence: %s",
        revisionsSinceEquivalence.size(),
        Joiner.on(", ").join(revisionsSinceEquivalence)));

    if (migrationConfig.getSeparateRevisions()) {
      ImmutableList.Builder<Migration> migrations = ImmutableList.builder();
      for (Revision fromRev : revisionsSinceEquivalence) {
        migrations.add(new Migration(migrationConfig, ImmutableList.of(fromRev), lastEq));
      }
      return migrations.build();
    } else {
      return ImmutableList.of(new Migration(migrationConfig, revisionsSinceEquivalence, lastEq));
    }
  }
}
