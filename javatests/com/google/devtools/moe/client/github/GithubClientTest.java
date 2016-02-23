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
package com.google.devtools.moe.client.github;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.Resources;
import com.google.devtools.moe.client.github.GithubAPI.IssueState;
import com.google.devtools.moe.client.github.GithubAPI.PullRequest;
import com.google.devtools.moe.client.github.GithubClient.OkHttpClientWrapper;
import com.google.gson.Gson;

import junit.framework.TestCase;

import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GithubClientTest extends TestCase {
  private static final String PULL_REQUEST_URL = "http://github.com/google/MOE/pull/14";

  private String pullRequestJson;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    URL resourcesUrl =
        getClass()
            .getClassLoader()
            .getResource("com/google/devtools/moe/client/github/pull_request.json");
    pullRequestJson = Resources.toString(resourcesUrl, StandardCharsets.UTF_8);
  }

  public void testGithubParsing() {
    OkHttpClientWrapper wrapper =
        new OkHttpClientWrapper(null) {
          @Override
          String getResponseJson(PullRequestUrl id) {
            return pullRequestJson;
          }
        };
    GithubClient clientToTest = new GithubClient(new Gson(), wrapper);

    PullRequest pullRequest = clientToTest.getPullRequest(PULL_REQUEST_URL);

    assertThat(pullRequest).isNotNull();
    assertThat(pullRequest.state()).isEqualTo(IssueState.OPEN);
    assertThat(pullRequest.head().sha()).isEqualTo("44691051251777694c47d769e5a7088aafe54ea4");
    assertThat(pullRequest.base().sha()).isEqualTo("7100bd630230e8c3702ee0a6c1976bba48f97964");
  }

  public void testCreatePullRequestId() {
    PullRequestUrl id = PullRequestUrl.create("http://github.com/foo/bar/pull/5");
    assertThat(id.owner()).isEqualTo("foo");
    assertThat(id.project()).isEqualTo("bar");
    assertThat(id.number()).isEqualTo(5);
    assertThat(id.apiAddress()).isEqualTo("https://api.github.com/repos/foo/bar/pulls/5");
  }

  public void testCreatePullRequestId_NonUrl() {
    try {
      PullRequestUrl.create("blah");
      fail("Expected exception");
    } catch (InvalidGithubUrl expected) {
      assertThat(expected.getMessage()).contains("url supplied is not a valid url: blah");
    }
  }

  public void testCreatePullRequestId_NonGithubUrl() {
    String url = "http://gitblubbr.com/blah/foo/pull/5";
    try {
      PullRequestUrl.create(url);
      fail("Expected exception");
    } catch (InvalidGithubUrl expected) {
      assertThat(expected.getMessage()).contains("not a github.com url");
      assertThat(expected.getMessage()).contains(url);
    }
  }

  public void testCreatePullRequestId_NonNumber() {
    String url = "http://github.com/blah/foo/pull/5a";
    try {
      PullRequestUrl.create(url);
      fail("Expected exception");
    } catch (InvalidGithubUrl expected) {
      assertThat(expected.getMessage()).contains("Invalid pull request number");
      assertThat(expected.getMessage()).contains(url);
    }
  }

  public void testCreatePullRequestId_NonPullRequestUrls() {
    try {
      PullRequestUrl.create("http://github.com/blah/foo/blargh/5");
      fail("Expected exception");
    } catch (InvalidGithubUrl expected) {
      assertThat(expected.getMessage()).contains("Invalid pull request URL: ");
    }
    try {
      PullRequestUrl.create("http://github.com/blah/foo/pulls");
      fail("Expected exception");
    } catch (InvalidGithubUrl expected) {
      assertThat(expected.getMessage()).contains("Invalid pull request URL: ");
    }
    try {
      PullRequestUrl.create("http://github.com/blah/foo/pull/5/foo");
      fail("Expected exception");
    } catch (InvalidGithubUrl expected) {
      assertThat(expected.getMessage()).contains("Invalid pull request URL: ");
    }
  }


}

