// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.codebase;

import com.google.devtools.moe.client.project.RepositoryConfig;

import java.io.File;

import javax.annotation.Nullable;

/**
 * Operations for objects encapsulating a local clone ('hg clone', 'git clone', 'svn checkout') of
 * a remote repository to disk.
 *
 */
public interface LocalClone {

  /**
   * @return the name of the Repository corresponding to this clone
   */
  String getRepositoryName();

  /**
   * @return the RepositoryConfig for the Repository corresponding to this clone
   */
  RepositoryConfig getConfig();

  /**
   * @return the root on disk of the local clone
   */
  File getLocalTempDir();

  /**
   * Clone the repo at repositoryUrl to disk. This method must update fields clonedLocally, revId,
   * and localCloneTempDir.
   */
  void cloneLocallyAtHead();

  /**
   * Update this clone to a given revision.
   */
  void updateToRevId(String revId);

  /**
   * Archive this clone (i.e. export a regular, versionless copy of its files) at a given revision.
   *
   * @param revId  the revision identifier (e.g. commit id) to archive at, null for head/tip
   *
   * @return the root of the archive
   */
  File archiveAtRevId(@Nullable String revId);
}
