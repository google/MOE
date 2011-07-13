// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.devtools.moe.client.codebase.Codebase;
import com.google.devtools.moe.client.database.Db;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.writer.DraftRevision;
import com.google.devtools.moe.client.writer.Writer;

import java.util.List;

/**
 * Perform the one_migration directive
 *
 */
public class OneMigrationLogic {

  /**
   * Perform a single migration
   *
   * @param db  the MOE Db storing equivalences
   * @param c the Codebase to use as source
   * @param destination the Writer to put the files from c into
   * @param revisionsToMigrate  all revisions to include in this migration (the metadata from the
   *                            Revisions in revisionsToMigrate determines the metadata for the
   *                            change)
   *
   * @return a DraftRevision on success, or null on failure
   */
  public static DraftRevision migrate(Db db, Codebase c, Writer destination,
                                      List<Revision> revisionsToMigrate) {
    //TODO(user): call determineMetadata on revisionsToMigrate, pass result to change
    DraftRevision r = ChangeLogic.change(c, destination);
    if (r == null) {
      return null;
    } else {
      //TODO(user): add new equivalence to db
      return r;
    }
  }
}
