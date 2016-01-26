/*
 * Copyright (c) 2011 Google, Inc.
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

package com.google.devtools.moe.client.testing;

import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.SystemUi;
import com.google.devtools.moe.client.Ui;

import dagger.Provides;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Ui that records what was reported to the user interface under the "info" level.
 */
public class RecordingUi extends SystemUi {
  public String lastInfo;
  public String lastError;
  public String lastTaskResult;

  public RecordingUi() {
    this(null);
  }

  @Inject
  public RecordingUi(@Nullable FileSystem fileSystem) {
    super(fileSystem);
    lastInfo = null;
    lastError = null;
  }

  @Override
  public void info(String msg, Object... args) {
    lastInfo = String.format(msg, args);
    super.info(msg, args);
  }

  @Override
  public void error(String msg, Object... args) {
    lastError = String.format(msg, args);
    super.error(msg, args);
  }

  @Override
  public void popTask(Ui.Task task, String result) {
    lastTaskResult = result;
    super.popTask(task, result);
  }

  /** A Dagger module for binding this implementation of {@link Ui}. */
  @dagger.Module
  public static class Module {
    @Provides
    @Singleton
    public Ui ui(RecordingUi impl) {
      return impl;
    }
  }
}
