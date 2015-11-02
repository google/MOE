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
