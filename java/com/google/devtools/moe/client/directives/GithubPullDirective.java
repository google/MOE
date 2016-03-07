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

package com.google.devtools.moe.client.directives;

import com.google.common.annotations.VisibleForTesting;
import com.google.devtools.moe.client.MoeProblem;
import com.google.devtools.moe.client.MoeUserProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.Ui.Task;
import com.google.devtools.moe.client.github.GithubAPI.PullRequest;
import com.google.devtools.moe.client.github.GithubClient;
import com.google.devtools.moe.client.github.PullRequestUrl;
import com.google.devtools.moe.client.project.ProjectConfig;
import com.google.devtools.moe.client.project.RepositoryConfig;

import dagger.Lazy;

import org.kohsuke.args4j.Option;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private final Lazy<ProjectConfig> config;
  private final GithubClient client;
  private final Ui ui;
  private final Lazy<MigrateBranchDirective> migrateBranchDirective;

  GithubPullDirective(
      Lazy<ProjectConfig> config,
      Ui ui,
      GithubClient client,
      Lazy<MigrateBranchDirective> migrateBranchDirective) {
    this.config = config;
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
      ui.message("DEBUG: Pull Request Metadata: '%s'", metadata);
    }
    if (metadata.merged()) {
      throw new MoeProblem("This pull request has already been merged on github: '%s'", url);
    }
    switch (metadata.mergeableState()) {
      case CLEAN:
        ui.message("Pull request %s is ready to merge", metadata.number());
        break;
      case UNSTABLE:
        ui.message(
            "WARNING: Pull request %s is ready to merge, but GitHub is reporting it as failing. "
                + "Continuing, but this branch may have problems. ",
            metadata.number());
        break;
      default:
        throw new MoeProblem(
            "Pull request %s is in an indeterminate state. "
                + "Please please check the pull request status, "
                + "perform any needed rebase/merge, and re-run",
            metadata.number());
    }

    MigrateBranchDirective delegate = migrateBranchDirective.get();

    String repoConfigName = findRepoConfig(config.get().repositories(), metadata);
    ui.message("Using '%s' as the source repository.", repoConfigName);
    int result =
        delegate.performBranchMigration(
            dbLocation,
            metadata.head().repo().owner().login() + "_" + metadata.head().ref(),
            repoConfigName,
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
  @VisibleForTesting
  static String findRepoConfig(
      final Map<String, RepositoryConfig> repositories, final PullRequest metadata) {
    PullRequestUrl id = PullRequestUrl.create(metadata.htmlUrl());
    for (Map.Entry<String, RepositoryConfig> entry : repositories.entrySet()) {
      if (isGithubRepositoryUrl(entry.getValue().getUrl(), id)) {
        return entry.getKey();
      }
    }
    throw new MoeUserProblem() {
      @Override
      public void reportTo(Ui messenger) {
        StringBuilder sb = new StringBuilder();
        sb.append("No configured repository is applicable to this pull request: ");
        sb.append(metadata.htmlUrl()).append("\n");
        for (Map.Entry<String, RepositoryConfig> entry : repositories.entrySet()) {
          sb.append("    name: ")
              .append(entry.getKey())
              .append(" url: ")
              .append(entry.getValue().getUrl())
              .append('\n');
        }
        messenger.message(sb.toString());
      }
    };
  }

  /**
   * A regular expression pattern that (roughly) matches the github repository url
   * possibilities, which are generally of three forms:<ul>
   *   <li>http://github.com/foo/bar
   *   <li>https://github.com/foo/bar
   *   <li>git@github.com:foo/bar
   * </ul>
   */
  private static final Pattern GITHUB_URL_PATTERN =
      Pattern.compile(".*github\\.com[:/]([a-zA-Z0-9_-]*)/([a-zA-Z0-9_-]*)(?:\\.git)?$");

  @VisibleForTesting
  static boolean isGithubRepositoryUrl(String url, PullRequestUrl pullRequestUrl) {
    if (url == null) {
      return false;
    }
    Matcher matcher = GITHUB_URL_PATTERN.matcher(url.trim());
    return matcher.matches()
        && matcher.group(1).equals(pullRequestUrl.owner())
        && matcher.group(2).equals(pullRequestUrl.project());
  }

  @Override
  public String getDescription() {
    return "Migrates the branch underlying a github pull request into a configured repository";
  }
}
