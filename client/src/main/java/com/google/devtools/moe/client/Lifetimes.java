/*
 * Copyright (c) 2012 Google, Inc.
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

import com.google.devtools.moe.client.FileSystem.Lifetime;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Static utility methods that return common {@link Lifetime}s. */
@Singleton
public final class Lifetimes {
  private final Ui ui;

  @Inject
  public Lifetimes(Ui ui) {
    this.ui = ui;
  }

  private static final Lifetime PERSISTENT =
      new Lifetime() {
        @Override
        public boolean shouldCleanUp() {
          return false;
        }
      };

  /**
   * Returns a {@code Lifetime} for a temp dir that should be cleaned up when the current {@link
   * Ui.Task} is completed.
   */
  public final Lifetime currentTask() {
    return ui.currentTaskLifetime();
  }

  /**
   * Returns a {@code Lifetime} for a temp dir that should only be cleaned up when MOE terminates.
   */
  public final Lifetime moeExecution() {
    return ui.moeExecutionLifetime();
  }

  /**
   * Returns a {@code Lifetime} for a temp dir that should never be cleaned up, even when MOE
   * terminates.
   */
  public static final Lifetime persistent() {
    return PERSISTENT;
  }
}
