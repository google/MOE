// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.codebase.CodebaseCreationError;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.parser.Expression;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.InvalidProject;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.project.ScrubberConfig;
import com.google.devtools.moe.client.repositories.MetadataScrubberConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;

import java.util.List;

/**
 * Perform the one_migration and migrate directives
 *
 */
public class OneMigrationLogic {

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
  public static DraftRevision migrate(
      Codebase c,
      Writer destination,
      List<Revision> revisionsToMigrate,
      ProjectContext context,
      Revision fromRevision,
      String fromRepository,
      String toRepository) {
    RevisionMetadata metadata =
        DetermineMetadataLogic.determine(context, revisionsToMigrate, fromRevision);
    metadata = scrubAuthors(metadata, context, fromRepository, toRepository);
    return ChangeLogic.change(c, destination, metadata);
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
  public static DraftRevision migrate(
      Migration migration,
      ProjectContext context,
      Writer destination,
      Expression referenceToCodebase) {

    Revision mostRecentFromRev = migration.fromRevisions.get(migration.fromRevisions.size() - 1);

    Codebase fromCodebase;
    try {
      String toProjectSpace =
          context.config.getRepositoryConfig(migration.config.getToRepository()).getProjectSpace();

      fromCodebase =
          new RepositoryExpression(migration.config.getFromRepository())
              .atRevision(mostRecentFromRev.revId)
              .translateTo(toProjectSpace)
              .withReferenceToCodebase(referenceToCodebase)
              .createCodebase(context);

    } catch (CodebaseCreationError e) {
      throw new MoeProblem(e.getMessage());
    }

    MetadataScrubberConfig sc = migration.config.getMetadataScrubberConfig();
    RevisionMetadata metadata =
        (sc == null)
            ? DetermineMetadataLogic.determine(context, migration.fromRevisions, mostRecentFromRev)
            : DetermineMetadataLogic.determine(
                context, migration.fromRevisions, sc, mostRecentFromRev);

    metadata =
        scrubAuthors(
            metadata,
            context,
            migration.config.getFromRepository(),
            migration.config.getToRepository());

    return ChangeLogic.change(fromCodebase, destination, metadata);
  }

  private static RevisionMetadata scrubAuthors(
      RevisionMetadata metadata,
      ProjectContext context,
      String fromRepository,
      String toRepository) {
    try {
      ScrubberConfig scrubber = context.config.findScrubberConfig(fromRepository, toRepository);
      if (scrubber != null && scrubber.shouldScrubAuthor(metadata.author)) {
        return new RevisionMetadata(
            metadata.id, null /* author */, metadata.date, metadata.description, metadata.parents);
      }
    } catch (InvalidProject exception) {
      throw new MoeProblem(exception.getMessage());
    }
    return metadata;
  }
}
