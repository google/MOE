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

package com.google.devtools.moe.client.translation.pipeline;

import com.google.auto.value.AutoValue;

/**
 * The Path a TranslationPipeline should take.
 */
@AutoValue
public abstract class TranslationPath {
  public abstract String fromProjectSpace();
  public abstract String toProjectSpace();

  public static TranslationPath create(String fromProjectSpace, String toProjectSpace) {
    return new AutoValue_TranslationPath(fromProjectSpace, toProjectSpace);
  }

  @Override
  public String toString() {
    return String.format("%s>%s", fromProjectSpace(), toProjectSpace());
  }
}
