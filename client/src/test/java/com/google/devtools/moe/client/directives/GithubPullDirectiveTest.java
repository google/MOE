/*
 * Copyright (c) 2015 Google, Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.moe.client.directives.GithubPullDirective.findRepoConfig;
import static com.google.devtools.moe.client.directives.GithubPullDirective.isGithubRepositoryUrl;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.devtools.moe.client.MoeUserProblem;
import com.google.devtools.moe.client.Ui;
import com.google.devtools.moe.client.github.GithubAPI.PullRequest;
import com.google.devtools.moe.client.github.PullRequestUrl;
import com.google.devtools.moe.client.GsonModule;
import com.google.devtools.moe.client.config.RepositoryConfig;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import junit.framework.TestCase;

/**
 * Tests for {@link GithubPullDirective}.
 */

public class GithubPullDirectiveTest extends TestCase {
  private static final Gson GSON = GsonModule.provideGson();

  public void testIsGithubUrlAndMapsToPull() throws Exception {
    assertThat(isGithubRepositoryUrl(null, null)).isFalse();
    assertThat(isGithubRepositoryUrl("", null)).isFalse();

    PullRequestUrl pr = PullRequestUrl.create("https://github.com/google/MOE/pull/305");
    assertThat(isGithubRepositoryUrl("https://github.com/google/MOE.git", pr)).isTrue();
    assertThat(isGithubRepositoryUrl("http://github.com/google/MOE.git", pr)).isTrue();
    assertThat(isGithubRepositoryUrl("git@github.com:google/MOE.git", pr)).isTrue();
    assertThat(isGithubRepositoryUrl("git@github.com:google/MOE", pr)).isTrue();
    assertThat(isGithubRepositoryUrl("https://github.com/google/MOE", pr)).isTrue();
    assertThat(isGithubRepositoryUrl("http://github.com/google/MOE", pr)).isTrue();

    pr = PullRequestUrl.create("https://github.com/google/closure-compiler/pull/3");
    assertThat(isGithubRepositoryUrl("https://github.com/google/closure-compiler.git", pr))
        .isTrue();
    assertThat(isGithubRepositoryUrl("https://github.com/google/closure-compiler", pr))
        .isTrue();

    pr = PullRequestUrl.create("https://github.com/foo/closure_compiler/pull/3");
    assertThat(isGithubRepositoryUrl("https://github.com/foo/closure_compiler.git", pr)).isTrue();

    pr = PullRequestUrl.create("https://github.com/truth0/plan9/pull/3");
    assertThat(isGithubRepositoryUrl("https://github.com/truth0/plan9.git", pr)).isTrue();

    assertThat(isGithubRepositoryUrl("https://bitbucket.com/google/MOE.git", null)).isFalse();
  }

  public void testIsGithubUrlDoesntMapToPull() throws Exception {
    PullRequestUrl pr = PullRequestUrl.create("https://github.com/google/MOE/pull/305");
    assertThat(isGithubRepositoryUrl("git@github.com:google/FOO.git", pr)).isFalse();
    assertThat(isGithubRepositoryUrl("git@github.com:cgruber/MOE", pr)).isFalse();
  }

  public void testValidRepoConfig() throws IOException {
    URL resourcesUrl =
        getClass()
            .getClassLoader()
            .getResource("com/google/devtools/moe/client/github/pull_request.json");
    String pullRequestJson = Resources.toString(resourcesUrl, StandardCharsets.UTF_8);
    PullRequest pr = GSON.fromJson(pullRequestJson, PullRequest.class);
    String config = "{\"name\":\"github\",\"url\":\"git@github.com:google/MOE.git\"}";
    Map<String, RepositoryConfig> repositories =
        ImmutableMap.of("github",  GSON.fromJson(config, RepositoryConfig.class));
    assertThat(findRepoConfig(repositories, pr)).isEqualTo("github");
  }

  public void testInvalidRepoConfig_differentProject() throws IOException {
    URL resourcesUrl =
        getClass()
            .getClassLoader()
            .getResource("com/google/devtools/moe/client/github/pull_request.json");
    String pullRequestJson = Resources.toString(resourcesUrl, StandardCharsets.UTF_8);
    PullRequest pr = GSON.fromJson(pullRequestJson, PullRequest.class);
    String config = "{\"name\":\"foo\",\"url\":\"git@github.com:google/test.git\"}";
    Map<String, RepositoryConfig> repositories =
        ImmutableMap.of("foo",  GSON.fromJson(config, RepositoryConfig.class));
    try {
      findRepoConfig(repositories, pr);
      fail("Should have thrown.");
    } catch (MoeUserProblem mup) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      Ui ui = new Ui(stream, /* fileSystem */ null);
      mup.reportTo(ui);
      String actualOutput = stream.toString();
      assertThat(actualOutput)
          .contains("No configured repository is applicable to this pull request");
      assertThat(actualOutput).contains("https://github.com/google/MOE/pull/14");
      assertThat(actualOutput).contains("name: foo");
    }
  }

}
