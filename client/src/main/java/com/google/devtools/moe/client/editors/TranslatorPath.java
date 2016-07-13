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

package com.google.devtools.moe.client.editors;

/**
 * The Path a Translator should take.
 */
// TODO(cgruber) @AutoValue
public class TranslatorPath {

  public final String fromProjectSpace;
  public final String toProjectSpace;

  public TranslatorPath(String fromProjectSpace, String toProjectSpace) {
    this.fromProjectSpace = fromProjectSpace;
    this.toProjectSpace = toProjectSpace;
  }

  @Override
  public String toString() {
    return String.format("%s>%s", fromProjectSpace, toProjectSpace);
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TranslatorPath)) {
      return false;
    }
    return toString().equals(o.toString());
  }
}
