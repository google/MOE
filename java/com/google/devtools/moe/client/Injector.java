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

import com.google.devtools.moe.client.project.ProjectContextFactory;

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

  @Nullable private final FileSystem FILE_SYSTEM;

  private final CommandRunner COMMAND;
  private final ProjectContextFactory CONTEXT_FACTORY;
  private final Ui UI;

  @Inject
  public Injector(
      @Nullable FileSystem fileSystem,
      CommandRunner command,
      ProjectContextFactory contextFactory,
      Ui ui) {
    this.FILE_SYSTEM = fileSystem;
    this.COMMAND = command;
    this.CONTEXT_FACTORY = contextFactory;
    this.UI = ui;
  }

  public CommandRunner getCommand() {
    return COMMAND;
  }

  public ProjectContextFactory getContextFactory() {
    return CONTEXT_FACTORY;
  }

  public FileSystem getFileSystem() {
    return FILE_SYSTEM;
  }

  public Ui getUi() {
    return UI;
  }
}
