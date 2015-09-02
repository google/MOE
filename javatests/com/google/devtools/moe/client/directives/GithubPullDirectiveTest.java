// Copyright 2015 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.directives;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.moe.client.directives.GithubPullDirective.isGithubRepositoryUrl;

import junit.framework.TestCase;

/**
 * Tests for {@link GithubPullDirective}.
 */
public class GithubPullDirectiveTest extends TestCase {
  public void testIsGithubUrl() throws Exception {
    assertThat(isGithubRepositoryUrl(null, null)).isFalse();
    assertThat(isGithubRepositoryUrl("", null)).isFalse();
    assertThat(isGithubRepositoryUrl("https://github.com/google/MOE.git", null)).isTrue();
    assertThat(isGithubRepositoryUrl("http://github.com/google/MOE.git", null)).isTrue();
    assertThat(isGithubRepositoryUrl("git@github.com:google/MOE.git", null)).isTrue();
    assertThat(isGithubRepositoryUrl("https://github.com/google/closure-compiler.git", null))
        .isTrue();
    assertThat(isGithubRepositoryUrl("https://github.com/foo/closure_compiler.git", null)).isTrue();
    assertThat(isGithubRepositoryUrl("https://github.com/truth0/plan9.git", null)).isTrue();
    assertThat(isGithubRepositoryUrl("https://github.com/google/MOE", null)).isFalse();
    assertThat(isGithubRepositoryUrl("https://bitbucket.com/google/MOE.git", null)).isFalse();
  }
}
