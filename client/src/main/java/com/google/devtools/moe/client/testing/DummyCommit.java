/*
 * Copyright (c) 2016 Google, Inc.
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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;

/**
 * Internal representation of a repository commit used within a {@link DummyRevisionHistory}.
 */
@AutoValue
public abstract class DummyCommit {
  public abstract String id();

  public abstract String author();

  public abstract String description();

  public abstract DateTime timestamp();

  public abstract ImmutableList<DummyCommit> parents();

  /** returns a commit containing the usual metadata, but with no ancestor(s) */
  public static DummyCommit create(
      String id, String author, String description, DateTime timestamp) {
    return create(id, author, description, timestamp, ImmutableList.<DummyCommit>of());
  }

  /** returns a commit containing the usual metadata and ancestor commit(s) */
  public static DummyCommit create(
      String id, String author, String description, DateTime timestamp, DummyCommit... parents) {
    return new AutoValue_DummyCommit(
        id, author, description, timestamp, ImmutableList.copyOf(parents));
  }

  /** returns a commit containing the usual metadata and a list of ancestor commit(s) */
  public static DummyCommit create(
      String id,
      String author,
      String description,
      DateTime timestamp,
      ImmutableList<DummyCommit> parents) {
    return new AutoValue_DummyCommit(id, author, description, timestamp, parents);
  }
}
