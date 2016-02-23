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
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.parser.Expression;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.TranslatorConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.tools.CodebaseDiffer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Logic behind keeping MOE db up to date (moe bookkeeping)
 */
public class Bookkeeper {
  /** The regex for MOE-migrated changes, as found in the changelog of the to-repo. */
  private static final Pattern MIGRATED_REV_PATTERN = Pattern.compile("MOE_MIGRATED_REVID=(\\S*)");

  private final CodebaseDiffer differ;
  private final Db.Writer dbWriter;
  private final Ui ui;


  @Inject
  public Bookkeeper(CodebaseDiffer differ, Db.Writer dbWriter, Ui ui) {
    this.differ = differ;
    this.dbWriter = dbWriter;
    this.ui = ui;
  }

  /**
   * Diff codebases at HEADs of fromRepository and toRepository, adding an Equivalence to db if
   * equivalent at HEADs.
   */
  private void updateHeadEquivalence(
      String fromRepository, String toRepository, Db db, ProjectContext context) {
    Codebase to, from;
    RevisionHistory fromHistory = context.getRepository(fromRepository).revisionHistory();
    RevisionHistory toHistory = context.getRepository(toRepository).revisionHistory();
    Revision toHead = toHistory.findHighestRevision(null);
    Revision fromHead = fromHistory.findHighestRevision(null);

    try {
      to =
          new RepositoryExpression(toRepository).atRevision(toHead.revId()).createCodebase(context);
      from =
          new RepositoryExpression(fromRepository)
              .atRevision(fromHead.revId())
              .translateTo(to.getProjectSpace())
              .createCodebase(context);
    } catch (CodebaseCreationError e) {
      throw new MoeProblem(e, "Could not generate codebase");
    }

    Ui.Task t = ui.pushTask("diff_codebases", "Diff codebases '%s' and '%s'", from, to);
    if (!differ.diffCodebases(from, to).areDifferent()) {
      db.noteEquivalence(RepositoryEquivalence.create(fromHead, toHead));
    }
    ui.popTask(t, "");
  }

  /**
   * Find Revisions in toRepository that were the result of a migration, and call
   * processMigration() on each.
   */
  private void updateCompletedMigrations(
      String fromRepository, String toRepository, Db db, ProjectContext context, boolean inverse) {

    RevisionHistory toHistory = context.getRepository(toRepository).revisionHistory();
    RepositoryEquivalenceMatcher.Result equivMatch =
        toHistory.findRevisions(
            null /*revision*/,
            new RepositoryEquivalenceMatcher(fromRepository, db),
            SearchType.LINEAR);

    List<Revision> linearToRevs =
        equivMatch.getRevisionsSinceEquivalence().getBreadthFirstHistory();
    ui.message(
        "Found %d revisions in %s since equivalence (%s): %s",
        linearToRevs.size(),
        toRepository,
        equivMatch.getEquivalences(),
        Joiner.on(", ").join(linearToRevs));

    for (Revision toRev : linearToRevs) {
      String fromRevId = getMigratedRevId(toHistory.getMetadata(toRev));
      if (fromRevId != null) {
        processMigration(Revision.create(fromRevId, fromRepository), toRev, db, context, inverse);
      }
    }
  }

  @Nullable
  private static String getMigratedRevId(RevisionMetadata metadata) {
    Matcher migratedRevMatcher = MIGRATED_REV_PATTERN.matcher(metadata.description);
    return migratedRevMatcher.find() ? migratedRevMatcher.group(1) : null;
  }

  /**
   * Check a submitted migration for equivalence by translating the from-repo to the to-repo, or
   * in the case of an inverse translation, translating the to-repo to the from-repo via the
   * forward-translator.
   */
  private void processMigration(
      Revision fromRev, Revision toRev, Db db, ProjectContext context, boolean inverse) {
    SubmittedMigration migration = SubmittedMigration.create(fromRev, toRev);
    if (!db.noteMigration(migration)) {
      ui.message(
          "Skipping bookkeeping of this SubmittedMigration because it was already in the Db: %s",
          migration);
      return;
    }

    Codebase to, from;
    try {
      Expression toEx = new RepositoryExpression(toRev.repositoryName()).atRevision(toRev.revId());
      Expression fromEx =
          new RepositoryExpression(fromRev.repositoryName()).atRevision(fromRev.revId());

      // Use the forward-translator to check an inverse-translated migration.
      if (inverse) {
        String fromProjectSpace =
            context.config().getRepositoryConfig(fromRev.repositoryName()).getProjectSpace();
        toEx = toEx.translateTo(fromProjectSpace);
      } else {
        String toProjectSpace =
            context.config().getRepositoryConfig(toRev.repositoryName()).getProjectSpace();
        fromEx = fromEx.translateTo(toProjectSpace);
      }

      to = toEx.createCodebase(context);
      from = fromEx.createCodebase(context);
    } catch (CodebaseCreationError e) {
      throw new MoeProblem(e, "Could not generate codebase");
    }

    Ui.Task t = ui.pushTask("diff_codebases", "Diff codebases '%s' and '%s'", from, to);
    if (!differ.diffCodebases(from, to).areDifferent()) {
      RepositoryEquivalence newEquiv = RepositoryEquivalence.create(fromRev, toRev);
      db.noteEquivalence(newEquiv);
      ui.message("Codebases are identical, noted new equivalence: %s", newEquiv);
    }
    ui.popTask(t, "");
  }

  /**
   * Look up the TranslatorConfig for translation of fromRepo to toRepo in the ProjectContext.
   */
  private static TranslatorConfig getTranslatorConfig(
      String fromRepo, String toRepo, ProjectContext context) {
    String fromProjectSpace = context.config().getRepositoryConfig(fromRepo).getProjectSpace();
    String toProjectSpace = context.config().getRepositoryConfig(toRepo).getProjectSpace();
    List<TranslatorConfig> transConfigs = context.config().translators();
    for (TranslatorConfig transConfig : transConfigs) {
      if (transConfig.getFromProjectSpace().equals(fromProjectSpace)
          && transConfig.getToProjectSpace().equals(toProjectSpace)) {
        return transConfig;
      }
    }
    throw new MoeProblem("Couldn't find a translator for " + fromRepo + " -> " + toRepo);
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
   * @param context the ProjectContext to evaluate in
   * @return 0 on success, 1 on failure
   */
  public int bookkeep(Db db, ProjectContext context) {
    Ui.Task t = ui.pushTask("perform_checks", "Updating database");

    for (MigrationConfig config : context.migrationConfigs().values()) {
      Ui.Task bookkeepOneMigrationTask =
          ui.pushTask(
              "bookkeping_one_migration",
              "Doing bookkeeping between '%s' and '%s' for migration '%s'",
              config.getFromRepository(),
              config.getToRepository(),
              config.getName());

      TranslatorConfig migrationTranslator =
          getTranslatorConfig(config.getFromRepository(), config.getToRepository(), context);

      // TODO(user): ? Switch the order of these two checks, so that we don't have to look back
      // through the history for irrelevant equivalences if there's one at head.
      Ui.Task checkMigrationsTask =
          ui.pushTask(
              "check_migrations",
              "Checking completed migrations for new equivalence between '%s' and '%s'",
              config.getFromRepository(),
              config.getToRepository());
      updateCompletedMigrations(
          config.getFromRepository(),
          config.getToRepository(),
          db,
          context,
          migrationTranslator.isInverse());
      ui.popTask(checkMigrationsTask, "");

      // Skip head-equivalence checking for inverse translation -- assume it will be performed via
      // the forward-translated migration.
      if (!migrationTranslator.isInverse()) {
        Ui.Task checkHeadsTask =
            ui.pushTask(
                "check_heads",
                "Checking head equivalence between '%s' and '%s'",
                config.getFromRepository(),
                config.getToRepository());
        updateHeadEquivalence(config.getFromRepository(), config.getToRepository(), db, context);
        ui.popTask(checkHeadsTask, "");
      }

      ui.popTask(bookkeepOneMigrationTask, "");
    }
    ui.popTask(t, "");
    dbWriter.write(db);
    return 0;
  }
}
