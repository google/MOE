// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.SystemUi;
import com.google.devtools.moe.client.Ui;

import dagger.Provides;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Ui that records what was Info'ed
 *
 * @author dbentley@google.com (Daniel Bentley)
 */
public class RecordingUi extends SystemUi {
  public String lastInfo;
  public String lastError;
  public String lastTaskResult;

  @Inject
  public RecordingUi() {
    lastInfo = null;
    lastError = null;
  }

  @Override
  public void info(String msg) {
    lastInfo = msg;
    super.info(msg);
  }

  @Override
  public void error(String msg) {
    lastError = msg;
    super.error(msg);
  }

  @Override
  public void popTask(Ui.Task task, String result) {
    lastTaskResult = result;
    super.popTask(task, result);
  }

  /** A Dagger module for binding this implementation of {@link Ui}. */
  @dagger.Module public static class Module {
    @Provides @Singleton public Ui ui(RecordingUi impl) {
      return impl;
    }
  }
}
