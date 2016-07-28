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

package com.google.devtools.moe.client;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A static class that acts as a sort of static holder for the key components.
 *
 * <p>This class is slated to be replaced more directly by a dagger component once
 * more elements of the code have eliminated the static reference and task-scope
 * is implemented.
 */
@Singleton
public class Injector {

  // TODO(cgruber): Eliminate this public static mutable.
  public static Injector INSTANCE;

  @Nullable private final FileSystem fileSystem;
  private final CommandRunner cmd;
  private final Ui ui;

  @Inject
  public Injector(
      @Nullable FileSystem fileSystem,
      CommandRunner cmd,
      Ui ui) {
    this.fileSystem = fileSystem;
    this.cmd = cmd;
    this.ui = ui;
  }

  public CommandRunner cmd() {
    return cmd;
  }

  public FileSystem fileSystem() {
    return fileSystem;
  }

  public Ui ui() {
    return ui;
  }
}
