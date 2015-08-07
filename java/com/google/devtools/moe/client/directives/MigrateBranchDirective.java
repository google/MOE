// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.database.RepositoryEquivalence;
import com.google.devtools.moe.client.migrations.Migration;
import com.google.devtools.moe.client.migrations.MigrationConfig;
import com.google.devtools.moe.client.migrations.Migrator;
import com.google.devtools.moe.client.parser.Expression;
import com.google.devtools.moe.client.parser.RepositoryExpression;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.repositories.ExactRevisionMatcher;
import com.google.devtools.moe.client.repositories.Repositories;
import com.google.devtools.moe.client.repositories.Repository;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionHistory;
import com.google.devtools.moe.client.repositories.RevisionHistory.SearchType;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;
import com.google.devtools.moe.client.writer.WritingError;

import org.kohsuke.args4j.Option;

import java.util.List;

import javax.inject.Inject;

/**
 * Perform a one-directional merge from a branch (and specified branch point) onto the head
 * of the target repository.
 *
 * <p>This is effectively works like the {@link MagicDirective} except that it only passes in one
 * direction, and does no bookkeeping.
 *
 * @author cgruber@google.com (Christian Gruber)
 */
public class MigrateBranchDirective extends Directive {
  @Option(name = "--from_repository", required = true, usage = "The label of the source repository")
  String fromRepository = "";

  @Option(name = "--branch", required = true, usage = "the symbolic name of the imported branch")
  String branchLabel = "";

  @Option(
      name = "--branch_root",
      required = true,
      usage = "the commit that constitutes the starting point for branch replay.")
  // TODO(cgruber) turn this into a Revision against the repo in question via an args factory
  String branchPoint = "";

  private Repositories repositories;
  private final Ui ui;
  private final Migrator oneMigrationLogic;

  @Inject
  MigrateBranchDirective(
      ProjectContextFactory contextFactory,
      Repositories repositories,
      Ui ui,
      Migrator oneMigrationLogic) {
    super(contextFactory);
    this.repositories = repositories;
    this.ui = ui;
    this.oneMigrationLogic = oneMigrationLogic;
  }

  @Override
  protected int performDirectiveBehavior() {
    List<MigrationConfig> configs =
        FluentIterable.from(context().migrationConfigs().values())
            .filter(
                new Predicate<MigrationConfig>() {
                  @Override
                  public boolean apply(MigrationConfig input) {
                    return input.getFromRepository().equals(fromRepository);
                  }
                })
            .toList();
    switch (configs.size()) {
      case 0:
        ui.error("No migration configurations could be found from repository '%s'", fromRepository);
        return 1;
      case 1:
        break;
      default:
        // TODO(cgruber) Allow specification of a migration if there are more than one.
        ui.error("More than one migration configuration from repository '%s'", fromRepository);
        return 1;
    }
    MigrationConfig migrationConfig = Iterables.getOnlyElement(configs);
    Ui.Task migrationTask =
        ui.pushTask(
            "perform_migration", "Performing migration '%s' from branch '%s'",
            migrationConfig.getName(),
            branchLabel);
    List<Migration> migrations = determineMigrations(migrationConfig);
    if (migrations.isEmpty()) {
      ui.info("No pending revisions to migrate in branch '%s' (as of %s)", branchLabel, "");
      return 0;
    }

    RepositoryExpression toRepo = new RepositoryExpression(migrationConfig.getToRepository());
    Writer toWriter;
    try {
      toWriter = toRepo.createWriter(context());
    } catch (WritingError e) {
      throw new MoeProblem("Couldn't create local repo %s: %s", toRepo, e);
    }

    DraftRevision dr = null; // Store one draft revision to obtain workspace location for UI.
    for (Migration m : migrations) {
      // For each migration, the reference to-codebase for inverse translation is the Writer,
      // since it contains the latest changes (i.e. previous migrations) to the to-repository.
      Expression referenceToCodebase =
          new RepositoryExpression(migrationConfig.getToRepository())
              .withOption("localroot", toWriter.getRoot().getAbsolutePath());

      Ui.Task oneMigrationTask =
          ui.pushTask(
              "perform_individual_migration",
              String.format("Performing individual migration '%s'", m.toString()));
      dr = oneMigrationLogic.migrate(m, context(), toWriter, referenceToCodebase);
      ui.popTask(oneMigrationTask, "");
    }
    toWriter.printPushMessage();
    ui.popTaskAndPersist(migrationTask, toWriter.getRoot());
    ui.info(
        "Created Draft workspace:\n%s in repository '%s'",
        dr.getLocation(),
        toRepo.getRepositoryName());
    return 0;
  }

  @Override
  public String getDescription() {
    return "Updates database and performs all migrations";
  }

  private List<Migration> determineMigrations(MigrationConfig migrationConfig) {
    String fromRepoName = migrationConfig.getFromRepository();
    RepositoryConfig fromRepoConfig =
        context().config().getRepositoryConfig(fromRepoName).copyWithBranch(branchLabel);
    Repository fromRepo = repositories.create(fromRepoName, fromRepoConfig);
    Revision branchPointRevision = Revision.create(branchPoint, fromRepoName);
    RevisionHistory history = fromRepo.revisionHistory();

    ExactRevisionMatcher.Result match =
        history.findRevisions(
            null, // Start at branch tip.
            new ExactRevisionMatcher(branchPointRevision),
            SearchType.LINEAR);
    // Since the matcher crawls from head, revisions() are in the opposite order from what
    // we want to replay.
    List<Revision> toMigrate = Lists.reverse(match.revisions().getBreadthFirstHistory());

    Revision toHead = context()
        .getRepository(migrationConfig.getToRepository())
        .revisionHistory()
        .findHighestRevision(null);
    RepositoryEquivalence migrationRoot = RepositoryEquivalence.create(branchPointRevision, toHead);

    ui.info(
        "Migrating %d revisions in %s (branch %s) from %s: %s",
        toMigrate.size(),
        migrationConfig.getFromRepository(),
        branchLabel,
        branchPointRevision,
        Joiner.on(", ").join(toMigrate));
    if (migrationConfig.getSeparateRevisions()) {
      ImmutableList.Builder<Migration> migrations = ImmutableList.builder();
      for (Revision fromRev : toMigrate) {
        migrations.add(new Migration(migrationConfig, ImmutableList.of(fromRev), migrationRoot));
      }
      return migrations.build();
    } else {
      return ImmutableList.of(new Migration(migrationConfig, toMigrate, migrationRoot));
    }
  }
}
