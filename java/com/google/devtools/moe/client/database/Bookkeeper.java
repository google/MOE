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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.TranslatorConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.tools.CodebaseDiffer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Logic behind keeping MOE db up to date (moe bookkeeping)
 */
public class Bookkeeper {
  private static final Logger logger = Logger.getLogger(Bookkeeper.class.getName());

  /** The regex for MOE-migrated changes, as found in the changelog of the to-repo. */
  private static final Pattern MIGRATED_REV_PATTERN = Pattern.compile("MOE_MIGRATED_REVID=(\\S*)");

  private final ProjectContext context;
  private final CodebaseDiffer differ;
  private final Db.Writer dbWriter;
  private final Ui ui;

  @Inject
  public Bookkeeper(ProjectContext context, CodebaseDiffer differ, Db.Writer dbWriter, Ui ui) {
    this.context = context;
    this.differ = differ;
    this.dbWriter = dbWriter;
    this.ui = ui;
  }

  private Revision head(String repositoryName) {
    return context.getRepository(repositoryName).revisionHistory().findHighestRevision(null);
  }

  /**
   * Diff codebases at HEADs of fromRepository and toRepository, adding an Equivalence to db if
   * equivalent at HEADs.
   *
   * @return a {@link RepositoryEquivalence} if the codebases at the given revisions are equivalent
   */
  private RepositoryEquivalence tryHeadEquivalence(
      Revision from, Revision to, Db db, boolean inverse) {
    Ui.Task checkHeadsTask =
        ui.pushTask(
            "checking head equivalency",
            "Checking head equivalence between '%s' and '%s'",
            from,
            to);
    RepositoryEquivalence equivalence =
        inverse ? determineEquivalence(to, from) : determineEquivalence(from, to);
    if (equivalence != null) {
      ui.message("SUCCESS: Found Equivalence between %s and %s", from, to);
      db.noteEquivalence(equivalence);
    } else {
      ui.message("No equivalence found between %s and %s", from, to);
    }
    ui.popTask(checkHeadsTask, equivalence != null ? "Found!!" : "Not Found.");
    return equivalence;
  }

  /**
   * Determines if the two revisions given are equivalent - that is to say, when fromRevision
   * is translated into the project space of toRevision, are their codebases without difference.
   *
   * @return a RepositoryEquivalence if the two codebases (after transformation) are equivalent.
   */
  private RepositoryEquivalence determineEquivalence(Revision fromRevision, Revision toRevision) {
    Codebase to, from;
    try {
      String toSpace =
          context.config().getRepositoryConfig(toRevision.repositoryName()).getProjectSpace();
      from =
          new RepositoryExpression(fromRevision.repositoryName())
              .atRevision(fromRevision.revId())
              .translateTo(toSpace)
              .createCodebase(context);
      to =
          new RepositoryExpression(toRevision.repositoryName())
              .atRevision(toRevision.revId())
              .createCodebase(context);
    } catch (CodebaseCreationError e) {
      throw new MoeProblem(e, "Could not generate codebase");
    }
    Ui.Task t = ui.pushTask("diff_codebases", "Diff codebases '%s' and '%s'", from, to);
    boolean equivalent = !differ.diffCodebases(from, to).areDifferent();
    ui.popTask(t, equivalent ? "No Difference" : "Difference Found");
    return equivalent ? RepositoryEquivalence.create(fromRevision, toRevision) : null;
  }

  /**
   * Find Revisions in toRepository that were the result of a migration and record them, noting
   * any found equivalences along the way.
   */
  private void noteCompletedMigrations(
      String fromRepository, String toRepository, Db db, boolean inverse) {
    Ui.Task checkMigrationsTask =
        ui.pushTask(
            "check_migrations",
            "Checking completed migrations for new equivalence between '%s' and '%s'",
            fromRepository,
            toRepository);

    RevisionHistory toHistory = context.getRepository(toRepository).revisionHistory();
    RepositoryEquivalenceMatcher.Result equivMatch =
        toHistory.findRevisions(
            null /*revision*/,
            new RepositoryEquivalenceMatcher(fromRepository, db),
            SearchType.BRANCHED);

    List<Revision> toRevs =
        equivMatch.getRevisionsSinceEquivalence().getBreadthFirstHistory();
    ui.message(
        "Found %d revisions in %s since equivalence (%s)",
        toRevs.size(),
        toRepository,
        equivMatch.getEquivalences());
    logger.fine("Revisions since equivalence: " + Joiner.on(" ").join(toRevs));
    int countUnmigrated = 0;
    int countProcessed = 0;
    for (Revision toRev : toRevs) {
      // Look up the migrated commit
      String fromRevId = getMigratedRevId(toHistory.getMetadata(toRev));
      if (fromRevId != null) {
        SubmittedMigration migration =
            SubmittedMigration.create(Revision.create(fromRevId, fromRepository), toRev);
        logger.fine("Processing submitted migration: " + migration);
        if (processMigration(migration, db, inverse) != null) {
          ui.message("Equivalence found - skipping remaining revisions in this migration.");
          countProcessed++; // Since we're short circuiting, count this commit as processed
          break;
        }
      } else {
        countUnmigrated++;
        logger.finer("Ignoring non-migrated revision " + toRev);
      }
      countProcessed++;
    }
    ui.message("Ignored %s commits that were not migrated by MOE", countUnmigrated);
    if (countProcessed < toRevs.size()) {
      ui.message(
          "Skipped %s commits that preceded a discovered migration",
          toRevs.size() - countProcessed);
    }
    ui.popTask(checkMigrationsTask, "");
  }

  @Nullable
  private static String getMigratedRevId(RevisionMetadata metadata) {
    Matcher migratedRevMatcher = MIGRATED_REV_PATTERN.matcher(metadata.description);
    return migratedRevMatcher.find() ? migratedRevMatcher.group(1) : null;
  }

  /**
   * Check a submitted migration for equivalence by translating the from-repo to the to-repo, or
   * in the case of an inverse translation, translating the to-repo to the from-repo via the
   * forward-translator.  A representation of that equivalence is returned, if one is found, or
   * null if no equivalence is found.
   */
  private RepositoryEquivalence processMigration(
      SubmittedMigration migration, Db db, boolean inverse) {
    if (!db.noteMigration(migration)) {
      ui.message(
          "Skipping: already recorded %s -> %s", migration.fromRevision(), migration.toRevision());
      return null;
    }
    Task t = ui.pushTask("process_migration", "Bookkeeping migrated revision %s", migration);
    // If an inverse translator, use the forward translator (but backwards)
    RepositoryEquivalence equivalence = (inverse)
        ? determineEquivalence(migration.toRevision(), migration.fromRevision())
        : determineEquivalence(migration.fromRevision(), migration.toRevision());
    if (equivalence != null) {
      db.noteEquivalence(equivalence);
      ui.message("SUCCESS: Equivalence found and recorded: %s", equivalence);
    }

    dbWriter.write(db);
    ui.popTask(t, "");
    return equivalence;
  }

  /**
   * Look up the TranslatorConfig for translation of fromRepo to toRepo in the ProjectContext.
   */
  private TranslatorConfig getTranslatorConfig(MigrationConfig migrationConfig) {
    String fromRepo = migrationConfig.getFromRepository();
    String toRepo = migrationConfig.getToRepository();
    String fromProjectSpace = context.config().getRepositoryConfig(fromRepo).getProjectSpace();
    String toProjectSpace = context.config().getRepositoryConfig(toRepo).getProjectSpace();
    List<TranslatorConfig> transConfigs = context.config().translators();
    for (TranslatorConfig transConfig : transConfigs) {
      if (transConfig.getFromProjectSpace().equals(fromProjectSpace)
          && transConfig.getToProjectSpace().equals(toProjectSpace)) {
        return transConfig;
      }
    }
    throw new MoeProblem("Couldn't find a translator for %s -> %s", fromRepo, toRepo);
  }

  /**
   * Looks for and adds to db SubmittedMigrations and Equivalences as the result of running one of
   * the directives Migrate or OneMigration, and the user commiting the result. Bookkeep only
   * considers Equivalences between repositories which are part of a migration listed both in
   * migrationNames and context.
   *
   * <p>Bookkeep should be run before performing any directive which reads from the db, since it is
   * MOE's way of keeping the db up-to-date.
   *
   * @param db the database to update
   * @return 0 on success, 1 on failure
   */
  public int bookkeep(Db db) {
    Ui.Task t = ui.pushTask("bookkeeping", "Updating database");

    Set<Set<Revision>> testedHeadEquivalences = new LinkedHashSet<>();
    for (MigrationConfig config : context.migrationConfigs().values()) {
      Ui.Task bookkeepOneMigrationTask =
          ui.pushTask(
              "bookkeeping " + config.getName(),
              "Doing bookkeeping between '%s' and '%s' for migration '%s'",
              config.getFromRepository(),
              config.getToRepository(),
              config.getName());

      TranslatorConfig translator = getTranslatorConfig(config);

      Revision fromHead = head(config.getFromRepository());
      Revision toHead = head(config.getToRepository());
      if (testedHeadEquivalences.add(ImmutableSet.of(fromHead, toHead))) {
        // If we haven't checked the inverse head-map of this pair, then check it, else skip.
        // This avoids checking head twice (once for forward mapping, once for inverse mapping)
        if (tryHeadEquivalence(fromHead, toHead, db, translator.isInverse()) != null) {
          // An equivalence at head was noted, don't bother noting all the intermediates.
          ui.popTask(bookkeepOneMigrationTask, "");
          continue;
        }
      }
      // Check each migration in turn, to see if we have an equivalence at some point in
      // the recent migration history.
      noteCompletedMigrations(
          config.getFromRepository(),
          config.getToRepository(),
          db,
          translator.isInverse());
      ui.popTask(bookkeepOneMigrationTask, "");
    }
    ui.popTask(t, "");
    dbWriter.write(db);
    return 0;
  }
}
