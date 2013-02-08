// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.common.base.Joiner;
import com.google.devtools.moe.client.AppContext;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.Equivalence;
import com.google.devtools.moe.client.database.EquivalenceMatcher;
import com.google.devtools.moe.client.database.EquivalenceMatcher.EquivalenceMatchResult;
import com.google.devtools.moe.client.database.SubmittedMigration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.parser.Expression;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.TranslatorConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.tools.CodebaseDifference;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Logic behind keeping MOE db up to date (moe bookkeeping)
 *
 */
public class BookkeepingLogic {

  /** The regex for MOE-migrated changes, as found in the changelog of the to-repo. */
  private static final Pattern MIGRATED_REV_PATTERN = Pattern.compile("MOE_MIGRATED_REVID=(\\S*)");

  /**
   * Diff codebases at HEADs of fromRepository and toRepository, adding an Equivalence to db if
   * equivalent at HEADs.
   */
  private static void updateHeadEquivalence(String fromRepository, String toRepository,
                                            Db db, ProjectContext context) {
    Codebase to, from;
    try {
      to = new RepositoryExpression(toRepository).createCodebase(context);
      from = new RepositoryExpression(fromRepository)
          .translateTo(to.getProjectSpace())
          .createCodebase(context);
    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error(e, "Could not generate codebase");
      return;
    }

    Ui.Task t = AppContext.RUN.ui.pushTask(
        "diff_codebases",
        String.format("Diff codebases '%s' and '%s'", from.toString(), to.toString()));
    if (!CodebaseDifference.diffCodebases(from, to).areDifferent()) {
      RevisionHistory fromHistory = context.getRepository(fromRepository).revisionHistory;
      RevisionHistory toHistory = context.getRepository(toRepository).revisionHistory;

      // TODO(user): Pull highest revision from the created codebases, not over again (in case
      // head has moved forward meanwhile).
      db.noteEquivalence(new Equivalence(fromHistory.findHighestRevision(null),
                                         toHistory.findHighestRevision(null)));
    }
    AppContext.RUN.ui.popTask(t, "");
  }

  /**
   * Find Revisions in toRepository that were the result of a migration, and call
   * processMigration() on each.
   */
  private static void updateCompletedMigrations(
      String fromRepository, String toRepository, Db db, ProjectContext context, boolean inverse) {

    RevisionHistory toHistory = context.getRepository(toRepository).revisionHistory;
    EquivalenceMatchResult equivMatch = toHistory.findRevisions(
        null /*revision*/,
        new EquivalenceMatcher(fromRepository, db),
        SearchType.LINEAR);

    List<Revision> linearToRevs =
        equivMatch.getRevisionsSinceEquivalence().getBreadthFirstHistory();
    AppContext.RUN.ui.info(String.format(
        "Found %d revisions in %s since equivalence (%s): %s",
        linearToRevs.size(),
        toRepository,
        equivMatch.getEquivalences(),
        Joiner.on(", ").join(linearToRevs)));

    for (Revision toRev : linearToRevs) {
      String fromRevId = getMigratedRevId(toHistory.getMetadata(toRev));
      if (fromRevId != null) {
        processMigration(new Revision(fromRevId, fromRepository), toRev, db, context, inverse);
      }
    }
  }

  private static @Nullable String getMigratedRevId(RevisionMetadata metadata) {
    Matcher migratedRevMatcher = MIGRATED_REV_PATTERN.matcher(metadata.description);
    return migratedRevMatcher.find() ? migratedRevMatcher.group(1) : null;
  }

  /**
   * Check a submitted migration for equivalence by translating the from-repo to the to-repo, or
   * in the case of an inverse translation, translating the to-repo to the from-repo via the
   * forward-translator.
   */
  private static void processMigration(Revision fromRev, Revision toRev,
                                       Db db, ProjectContext context, boolean inverse) {
    SubmittedMigration migration = new SubmittedMigration(fromRev, toRev);
    if (!db.noteMigration(migration)) {
      AppContext.RUN.ui.info("Skipping bookkeeping of this SubmittedMigration "
          + "because it was already in the Db: " + migration);
      return;
    }

    Codebase to, from;
    try {
      Expression toEx =
          new RepositoryExpression(toRev.repositoryName).atRevision(toRev.revId);
      Expression fromEx =
          new RepositoryExpression(fromRev.repositoryName).atRevision(fromRev.revId);

      // Use the forward-translator to check an inverse-translated migration.
      if (inverse) {
        String fromProjectSpace =
            context.config.getRepositoryConfig(fromRev.repositoryName).getProjectSpace();
        toEx = toEx.translateTo(fromProjectSpace);
      } else {
        String toProjectSpace =
            context.config.getRepositoryConfig(toRev.repositoryName).getProjectSpace();
        fromEx = fromEx.translateTo(toProjectSpace);
      }

      to = toEx.createCodebase(context);
      from = fromEx.createCodebase(context);

    } catch (CodebaseCreationError e) {
      AppContext.RUN.ui.error(e, "Could not generate codebase");
      return;
    }

    Ui.Task t = AppContext.RUN.ui.pushTask(
        "diff_codebases",
        String.format("Diff codebases '%s' and '%s'", from.toString(), to.toString()));
    if (!CodebaseDifference.diffCodebases(from, to).areDifferent()) {
      Equivalence newEquiv = new Equivalence(fromRev, toRev);
      db.noteEquivalence(newEquiv);
      AppContext.RUN.ui.info("Codebases are identical, noted new equivalence: " + newEquiv);
    }
    AppContext.RUN.ui.popTask(t, "");
  }

  /**
   * Look up the TranslatorConfig for translation of fromRepo to toRepo in the ProjectContext.
   */
  private static TranslatorConfig getTranslatorConfig(
      String fromRepo, String toRepo, ProjectContext context) {
    String fromProjectSpace =
        context.config.getRepositoryConfig(fromRepo).getProjectSpace();
    String toProjectSpace = context.config.getRepositoryConfig(toRepo).getProjectSpace();
    List<TranslatorConfig> transConfigs = context.config.getTranslators();
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
   * Bookkeep should be run before performing any directive which reads from the db, since it is
   * MOE's way of keeping the db up-to-date.
   *
   * @param migrationNames the names of all migrations
   * @param db  the database to update
   * @param dbLocation  where db is located
   * @param context the ProjectContext to evaluate in
   * @return  0 on success, 1 on failure
   */
  public static int bookkeep(List<String> migrationNames, Db db, String dbLocation,
                             ProjectContext context) {
    Ui.Task t = AppContext.RUN.ui.pushTask("perform_checks", "Updating database");
    for (String s : migrationNames) {
      MigrationConfig m = context.migrationConfigs.get(s);
      if (m == null) {
        AppContext.RUN.ui.error(String.format("No migration '%s' in MOE config", s));
        return 1;
      }

      Ui.Task bookkeepOneMigrationTask = AppContext.RUN.ui.pushTask(
          "bookkeping_one_migration",
          String.format("Doing bookkeeping between '%s' and '%s' for migration '%s'",
                        m.getFromRepository(), m.getToRepository(), m.getName()));

      TranslatorConfig migrationTranslator =
          getTranslatorConfig(m.getFromRepository(), m.getToRepository(), context);

      // TODO(user): ? Switch the order of these two checks, so that we don't have to look back
      // through the history for irrelevant equivalences if there's one at head.
      Ui.Task checkMigrationsTask = AppContext.RUN.ui.pushTask(
          "check_migrations",
          String.format(
              "Checking completed migrations for new equivalence between '%s' and '%s'",
              m.getFromRepository(), m.getToRepository()));
      updateCompletedMigrations(
          m.getFromRepository(), m.getToRepository(), db, context,
          migrationTranslator.isInverse());
      AppContext.RUN.ui.popTask(checkMigrationsTask, "");

      // Skip head-equivalence checking for inverse translation -- assume it will be performed via
      // the forward-translated migration.
      if (!migrationTranslator.isInverse()) {
        Ui.Task checkHeadsTask = AppContext.RUN.ui.pushTask(
            "check_heads",
            String.format(
                "Checking head equivalence between '%s' and '%s'",
                m.getFromRepository(), m.getToRepository()));
        updateHeadEquivalence(m.getFromRepository(), m.getToRepository(), db, context);
        AppContext.RUN.ui.popTask(checkHeadsTask, "");
      }

      AppContext.RUN.ui.popTask(bookkeepOneMigrationTask, "");
    }
    AppContext.RUN.ui.popTask(t, "");
    db.writeToLocation(dbLocation);
    return 0;
  }
}
