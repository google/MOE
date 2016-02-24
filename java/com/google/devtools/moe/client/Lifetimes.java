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

/**
 * Static utility methods that return common {@link Lifetime}s.
 */
public final class Lifetimes {

  private Lifetimes() {} // Do not instantiate.

  private static final Lifetime PERSISTENT =
      new Lifetime() {
        @Override
        public boolean shouldCleanUp() {
          return false;
        }
      };

  /**
   * Gets the current {@code Lifetime} for a temporary directory that should be 
   * cleaned up when the current {@link Ui.Task} is completed.
   * 
   * @return the current task lifetime.
   */
  public static final Lifetime currentTask() {
    return Injector.INSTANCE.getUi().getCurrentTaskLifetime();
  }

  /**
   * Gets a {@code Lifetime} for a temporary directory that should only be 
   * cleaned up when MOE terminates.
   * 
   * @return the lifetime for the MOE execution.
   */
  public static final Lifetime moeExecution() {
    return Injector.INSTANCE.getUi().getMoeExecutionLifetime();
  }

  /**
   * Returns a {@code Lifetime} for a temporary directory that should never be 
   * cleaned up, even when MOE terminates.
   * 
   * @return the lifetime for the MOE execution.
   */
  public static final Lifetime persistent() {
    return PERSISTENT;
  }
}
