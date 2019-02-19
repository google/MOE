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

package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.config.RepositoryConfig;
import java.io.File;
import javax.annotation.Nullable;

/**
 * Operations for objects encapsulating a local workspace/clone/client of a repository
 * (e.g. {@code hg clone}, {@code git clone}, {@code svn checkout}).
 */
public interface LocalWorkspace {

  /**
   * Returns the name of the cloned
   * {@link com.google.devtools.moe.client.repositories.RepositoryType}.
   */
  String getRepositoryName();

  /**
   * Returns the RepositoryConfig for the Repository corresponding to this clone.
   */
  RepositoryConfig getConfig();

  /**
   * Returns the root on disk of the local clone.
   */
  File getLocalTempDir();

  /**
   * Clones the repo from its remote location to disk at head revision. The clone's temporary
   * directory has the given {code Lifetime}.
   */
  void cloneLocallyAtHead(Lifetime cloneLifetime);

  /**
   * Updates this clone to a given revision.
   */
  void updateToRevision(String revId);

  /**
   * Archives this clone. An archive is an unversioned copy (an expanded directory) of the cloned
   * codebase. The clone is archived at the given revision if {@code revId} is non-null, or at
   * head otherwise.
   *
   * @param revId  the revision identifier (e.g. commit id) to archive at, null for head
   * @return the root of the archive
   */
  File archiveAtRevision(@Nullable String revId);
}
