// Copyright 2015 The MOE Authors All Rights Reserved.
package com.google.devtools.moe.client.github;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A representation of a pull request URL, that provides validation and conversion to the
 * Github restful API equivalent URL.
 */
@AutoValue
public abstract class PullRequestUrl {
  public abstract String owner();

  public abstract String project();

  public abstract int number();

  /**
   * Create from a URL string, throwing an {@link InvalidGithubUrl} exception if the supplied
   * URL is invalid or cannot be parsed as a URL.
   */
  public static PullRequestUrl create(String url) {
    try {
      return create(new URL(url));
    } catch (MalformedURLException unused) {
      throw new InvalidGithubUrl("Pull request url supplied is not a valid url: " + url);
    }
  }

  /**
   * Create from a URL string, throwing an {@link InvalidGithubUrl} exception if the supplied
   * URL is invalid.
   */
  static PullRequestUrl create(URL url) {
    if (!url.getHost().equals("github.com")) {
      throw new InvalidGithubUrl("Pull request url is not a github.com url: '%s'", url);
    }
    ImmutableList<String> path = ImmutableList.copyOf(url.getPath().substring(1).split("/"));
    if (path.size() != 4 || !path.get(2).equals("pull")) {
      throw new InvalidGithubUrl("Invalid pull request URL: '%s'", url);
    }
    try {
      return new AutoValue_PullRequestUrl(path.get(0), path.get(1), Integer.parseInt(path.get(3)));
    } catch (NumberFormatException nfe) {
      throw new InvalidGithubUrl("Invalid pull request number '%s': '%s'", path.get(3), url);
    }
  }

  String apiAddress() {
    return "https://api.github.com/repos/" + owner() + "/" + project() + "/pulls/" + number();
  }
}
