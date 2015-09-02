// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import com.google.common.annotations.VisibleForTesting;
import com.google.devtools.moe.client.Messenger;
import com.google.devtools.moe.client.MoeUserProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.github.GithubAPI.PullRequest;
import com.google.devtools.moe.client.github.GithubClient;
import com.google.devtools.moe.client.github.PullRequestUrl;
import com.google.devtools.moe.client.project.ProjectContextFactory;
import com.google.devtools.moe.client.project.RepositoryConfig;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import java.util.Map;

/**
 * Extracts branch metadata from a github pull request and uses this information to
 * perform a branch migration, replaying the pull request commits onto the head of
 * the migration target.
 *
 * @see MigrateBranchDirective
 */
public class GithubPullDirective extends Directive {
  @Option(name = "--db", required = true, usage = "Location of MOE database")
  String dbLocation = "";

  @Option(
    name = "--url",
    required = true,
    usage = "The URL of the pull request\n(e.g. https://github.com/google/MOE/pull/32)"
  )
  String url = "";

  private final GithubClient client;
  private final Ui ui;
  private final Lazy<MigrateBranchDirective> migrateBranchDirective;

  GithubPullDirective(
      ProjectContextFactory contextFactory,
      Ui ui,
      GithubClient client,
      Lazy<MigrateBranchDirective> migrateBranchDirective) {
    super(contextFactory); // TODO(cgruber) Inject project context, not its factory
    this.client = client;
    this.ui = ui;
    this.migrateBranchDirective = migrateBranchDirective;
  }

  @Override
  protected int performDirectiveBehavior() {
    Task task =
        ui.pushTask("github-import", "Import a github pull-request and stage it in a workspace.");
    PullRequest metadata = client.getPullRequest(url);
    if (debug()) {
      // TODO(cgruber) Re-work debug mode into ui messenger when flags are separated from directives
      ui.info("DEBUG: Pull Request Metadata: '%s'", metadata);
    }
    if (metadata.merged()) {
      ui.error("This pull request has already been merged on github: '%s'", url);
      return 1;
    }
    switch (metadata.mergeableState()) {
      case CLEAN:
        ui.info("Pull request %s is ready to merge", metadata.number());
        break;
      case UNSTABLE:
        ui.info(
            "WARNING: Pull request %s is ready to merge, but GitHub is reporting it as failing. "
                + "Continuing, but this branch may have problems. ",
            metadata.number());
        break;
      default:
        ui.error(
            "Pull request %s is in an indeterminate state. "
                + "Please please check the pull request status, "
                + "perform any needed rebase/merge, and re-run",
            metadata.number());
        return 1;
    }

    MigrateBranchDirective delegate = migrateBranchDirective.get();

    // TODO(cgruber): Strip out project context so that this is not necessary.
    // Override context to avoid re-creating it from files.
    delegate.context = context;

    int result =
        delegate.performBranchMigration(
            dbLocation,
            findRepoConfig(metadata),
            metadata.head().ref(),
            metadata.head().repo().cloneUrl());
    if (delegate.resultDirectory != null) {
      ui.popTaskAndPersist(task, delegate.resultDirectory);
    } else {
      ui.popTask(task, "");
    }
    return result;
  }

  /**
   * Finds a repository configuration from among the configured repositories that match the
   * supplied pull request metadata, if any.
   */
  private String findRepoConfig(PullRequest metadata) {
    PullRequestUrl id = PullRequestUrl.create(metadata.htmlUrl());
    for (Map.Entry<String, RepositoryConfig> entry : context().config().repositories().entrySet()) {
      if (isGithubRepositoryUrl(entry.getValue().getUrl(), id)) {
        ui.info("Using '%s' as the source repository.", entry.getKey());
        return entry.getKey();
      }
    }
    throw new MoeUserProblem() {
      @Override
      public void reportTo(Messenger messenger) {
        messenger.error("None of the configured repositories is a github repository.");
        for (Map.Entry<String, RepositoryConfig> entry :
            context().config().repositories().entrySet()) {
          messenger.info("    name: %s url: %s", entry.getKey(), entry.getValue().getUrl());
        }
      }
    };
  }

  @VisibleForTesting
  static boolean isGithubRepositoryUrl(String url, PullRequestUrl unused) {
    // TODO(cgruber) validate against req using pattern capture groups.
    return url != null
        && (url.trim().matches("https?+://github[.]com/[a-zA-Z0-9_-]*/[a-zA-Z0-9_-]*[.]git")
            || url.trim().matches("git@github[.]com:[a-zA-Z0-9_-]*/[a-zA-Z0-9_-]*[.]git"));
  }

  @Override
  public String getDescription() {
    return "Migrates the branch underlying a github pull request into a configured repository";
  }
}
