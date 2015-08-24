// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.migrations;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.Injector;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher;
import com.google.devtools.moe.client.database.RepositoryEquivalenceMatcher.Result;
import com.google.devtools.moe.client.parser.Expression;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ScrubberConfig;
import com.google.devtools.moe.client.repositories.MetadataScrubber;
import com.google.devtools.moe.client.repositories.MetadataScrubberConfig;
import com.google.devtools.moe.client.repositories.RepositoryType;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Perform the one_migration and migrate directives
 *
 */
public class Migrator {
  private final DraftRevision.Factory revisionFactory;

  @Inject
  public Migrator(DraftRevision.Factory revisionFactory) {
    this.revisionFactory = revisionFactory;
  }

  /**
   * Perform a migration from specified source, destination, and specified Revisions
   *
   * @param c the Codebase to use as source
   * @param destination the Writer to put the files from c into
   * @param revisionsToMigrate  all revisions to include in this migration (the metadata from the
   *                            Revisions in revisionsToMigrate determines the metadata for the
   *                            change)
   * @param context the context to evaluate in
   *
   * @return  a DraftRevision on success, or null on failure
   */
  public DraftRevision migrate(
      Codebase c,
      Writer destination,
      List<Revision> revisionsToMigrate,
      ProjectContext context,
      Revision fromRevision,
      String fromRepository,
      String toRepository) {
    RevisionMetadata metadata = processMetadata(context, revisionsToMigrate, null, fromRevision);
    metadata = scrubAuthors(metadata, context, fromRepository, toRepository);
    return revisionFactory.create(c, destination, metadata);
  }

  /**
   * Perform a migration from a Migration object. Includes metadata scrubbing.
   *
   * The DraftRevision is created at the revision of last equivalence, or from the head/tip of the
   * repository if no Equivalence could be found.
   *
   * @param migration the Migration representing the migration to perform
   * @param context the context to evaluate in
   * @param destination the Writer to put the changes from the Migration into
   * @param referenceToCodebase the reference to-codebase Expression used in case this Migration is
   *                            an inverse translation
   *
   * @return  a DraftRevision on success, or null on failure
   */
  public DraftRevision migrate(
      Migration migration,
      ProjectContext context,
      Writer destination,
      Expression referenceToCodebase) {

    Revision mostRecentFromRev = migration.fromRevisions.get(migration.fromRevisions.size() - 1);

    Codebase fromCodebase;
    try {
      String toProjectSpace =
          context
              .config()
              .getRepositoryConfig(migration.config.getToRepository())
              .getProjectSpace();

      fromCodebase =
          new RepositoryExpression(migration.config.getFromRepository())
              .atRevision(mostRecentFromRev.revId())
              .translateTo(toProjectSpace)
              .withReferenceToCodebase(referenceToCodebase)
              .createCodebase(context);

    } catch (CodebaseCreationError e) {
      throw new MoeProblem(e.getMessage());
    }

    MetadataScrubberConfig sc = migration.config.getMetadataScrubberConfig();
    RevisionMetadata metadata =
        processMetadata(context, migration.fromRevisions, sc, mostRecentFromRev);

    metadata =
        scrubAuthors(
            metadata,
            context,
            migration.config.getFromRepository(),
            migration.config.getToRepository());

    return revisionFactory.create(fromCodebase, destination, metadata);
  }

  private RevisionMetadata scrubAuthors(
      RevisionMetadata metadata,
      ProjectContext context,
      String fromRepository,
      String toRepository) {
    try {
      ScrubberConfig scrubber = context.config().findScrubberConfig(fromRepository, toRepository);
      if (scrubber != null && scrubber.shouldScrubAuthor(metadata.author)) {
        return new RevisionMetadata(
            metadata.id, null /* author */, metadata.date, metadata.description, metadata.parents);
      }
    } catch (InvalidProject exception) {
      throw new MoeProblem(exception.getMessage());
    }
    return metadata;
  }

  /**
   * @param migrationConfig  the migration specification
   * @param db  the MOE db which will be used to find the last equivalence
   * @return a list of pending Migrations since last {@link RepositoryEquivalence} per
   *     migrationConfig
   */
  public List<Migration> determineMigrations(
      ProjectContext context, MigrationConfig migrationConfig, Db db) {

    RepositoryType fromRepo = context.getRepository(migrationConfig.getFromRepository());
    // TODO(user): Decide whether to migrate linear or graph history here. Once DVCS Writers
    // support writing a graph of Revisions, we'll need to opt for linear or graph history based
    // on the MigrationConfig (e.g. whether or not the destination repo is linear-only).
    Result equivMatch =
        fromRepo
            .revisionHistory()
            .findRevisions(
                null, // Start at head.
                new RepositoryEquivalenceMatcher(migrationConfig.getToRepository(), db),
                SearchType.LINEAR);

    List<Revision> revisionsSinceEquivalence =
        Lists.reverse(equivMatch.getRevisionsSinceEquivalence().getBreadthFirstHistory());

    if (revisionsSinceEquivalence.isEmpty()) {
      Injector.INSTANCE
          .ui()
          .info(
              "No revisions found since last equivalence for migration '"
                  + migrationConfig.getName()
                  + "'");
      return ImmutableList.of();
    }

    // TODO(user): Figure out how to report all equivalences.
    RepositoryEquivalence lastEq = equivMatch.getEquivalences().get(0);
    Injector.INSTANCE
        .ui()
        .info(
            "Found %d revisions in %s since equivalence (%s): %s",
            revisionsSinceEquivalence.size(),
            migrationConfig.getFromRepository(),
            lastEq,
            Joiner.on(", ").join(revisionsSinceEquivalence));

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

  /**
   * Get and scrub RevisionMetadata based on the given MetadataScrubberConfig.
   */
  public RevisionMetadata processMetadata(
      ProjectContext context,
      List<Revision> revs,
      @Nullable MetadataScrubberConfig sc,
      @Nullable Revision fromRevision) {
    ImmutableList.Builder<RevisionMetadata> rmBuilder = ImmutableList.builder();
    List<MetadataScrubber> scrubbers =
        (sc == null) ? ImmutableList.<MetadataScrubber>of() : sc.getScrubbers();

    for (Revision rev : revs) {
      RevisionMetadata rm =
          context.getRepository(rev.repositoryName()).revisionHistory().getMetadata(rev);
      for (MetadataScrubber scrubber : scrubbers) {
        rm = scrubber.scrub(rm);
      }
      rmBuilder.add(rm);
    }

    return RevisionMetadata.concatenate(rmBuilder.build(), fromRevision);
  }
}
