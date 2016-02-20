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
package com.google.devtools.moe.client.tools;

import dagger.Lazy;

/** An implementation of {@link dagger.Lazy} used for testing (and in one legacy position) */
// TODO(cgruber) move to testing this when all directives inject Lazy<ProjectContext>
public final class EagerLazy<T> implements Lazy<T> {
  private final T instance;

  private EagerLazy(T instance) {
    this.instance = instance;
  }

  @Override
  public T get() {
    return instance;
  }

  public static <T> Lazy<T> fromInstance(T instance) {
    return new EagerLazy<T>(instance);
  }
}
