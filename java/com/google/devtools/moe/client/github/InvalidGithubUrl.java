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

import com.google.devtools.moe.client.MoeUserProblem;
import com.google.devtools.moe.client.Ui;

/** An error reported when an invalid github pull request URL is given */
public class InvalidGithubUrl extends MoeUserProblem {
  private final String message;

  InvalidGithubUrl(String messageFmt, Object... args) {
    message = String.format(messageFmt, args);
  }

  @Override
  public void reportTo(Ui messenger) {
    messenger.message(message);
  }
}
