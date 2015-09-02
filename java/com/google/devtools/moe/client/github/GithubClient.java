
// Copyright 2015 The MOE Authors All Rights Reserved.
package com.google.devtools.moe.client.github;

import com.google.devtools.moe.client.github.GithubAPI.PullRequest;
import com.google.gson.Gson;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

import javax.inject.Inject;

/**
 * A client for accessing github APIs
 */
public class GithubClient {
  private final Gson gson;
  private final OkHttpClientWrapper httpClient;

  @Inject
  GithubClient(Gson gson, OkHttpClientWrapper httpClient) {
    this.gson = gson;
    this.httpClient = httpClient;
  }

  /**
   * Issues a blocking request to the GitHub API to supply all of the metadata around
   * a given pull request url, populating a {@link PullRequest} instance (and all of its
   * contained classes);
   */
  public PullRequest getPullRequest(String pullRequestUrl) {
    PullRequestUrl req = PullRequestUrl.create(pullRequestUrl);
    String jsonString = httpClient.getResponseJson(req);
    return gson.fromJson(jsonString, PullRequest.class);
  }

  /** A thin wrapper around {@link OkHttpClient} to make testing a bit cleaner. */
  static class OkHttpClientWrapper {
    private final OkHttpClient client;

    @Inject
    OkHttpClientWrapper(OkHttpClient client) {
      this.client = client;
    }

    String getResponseJson(PullRequestUrl id) {
      Request request =
          new Request.Builder()
              .url(id.apiAddress())
              .addHeader("User-Agent", "OkHttpClient/1.0 (Make Open Easy repository sync software)")
              .build();
      try {
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
          switch (response.code()) {
            case 404:
              throw new InvalidGithubUrl("No such pull request found: %s", id);
            case 403:
              throw new InvalidGithubUrl("Github rate-limit reached - please wait 60 mins: %s", id);
            default:
              throw new IOException("Unexpected code " + response);
          }
        }
        return response.body().string();
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }
}

