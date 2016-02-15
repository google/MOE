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
 * A specification of whether a temporary directory should be cleaned up on a call to
 * {@link FileSystem#cleanUpTempDirs()}. On clean-up, each temporary directory's {@code Lifetime}
 * is looked up, and {@link #shouldCleanUp()} is called.
 */
public interface Lifetime {

  /**
   * Checks if a temporary directory with this {@code Lifetime} should be cleaned up now.
   *
   * @return true if the directory should be cleaned up now or false, otherwise.
   * @see FileSystem#cleanUpTempDirs()
   */
  boolean shouldCleanUp();
  
}
