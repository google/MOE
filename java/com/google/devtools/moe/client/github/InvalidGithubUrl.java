// Copyright 2015 The MOE Authors All Rights Reserved.
package com.google.devtools.moe.client.github;

import com.google.devtools.moe.client.Messenger;
import com.google.devtools.moe.client.MoeUserProblem;

/** An error reported when an invalid github pull request URL is given */
public class InvalidGithubUrl extends MoeUserProblem {
  private final String message;

  InvalidGithubUrl(String messageFmt, Object... args) {
    message = String.format(messageFmt, args);
  }

  @Override
  public void reportTo(Messenger messenger) {
    messenger.info(message);
  }
}
