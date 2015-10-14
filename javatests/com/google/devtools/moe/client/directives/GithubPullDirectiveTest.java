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
