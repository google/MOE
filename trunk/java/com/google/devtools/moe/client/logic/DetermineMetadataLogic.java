// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.MetadataScrubber;
import com.google.devtools.moe.client.repositories.MetadataScrubberConfig;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Performs the logic of the DetermineMetadataDirective.
 *
 */
public class DetermineMetadataLogic {

  /**
   * Makes a RevisionMetadata for the given Revisions.
   *
   * @param context  the context to evaluate in
   * @param revs  the Revisions to include
   * @param fromRevision  when this method is used to get metadata for a migration, a Revision
   *                      describing the Revision in the from repository, otherwise null
   * @return RevisionMetadata concatenating the metadata of the given revs
   */
  public static RevisionMetadata determine(ProjectContext context,
                                           List<Revision> revs,
                                           @Nullable Revision fromRevision) {
    return determine(context, revs, null /*MetadataScrubberConfig sc*/, fromRevision);
  }

  /**
   * Get and scrub RevisionMetadata based on the given MetadataScrubberConfig.
   */
  // TODO(user): Simplify DetermineMetadata to a single interface devoid of @Nullable args.
  public static RevisionMetadata determine(ProjectContext context,
                                           List<Revision> revs,
                                           @Nullable MetadataScrubberConfig sc,
                                           @Nullable Revision fromRevision) {
    ImmutableList.Builder<RevisionMetadata> rmBuilder = ImmutableList.builder();
    List<MetadataScrubber> scrubbers =
        (sc == null) ? ImmutableList.<MetadataScrubber>of() : sc.getScrubbers();

    for (Revision rev : revs) {
      RevisionMetadata rm =
          context.repositories.get(rev.repositoryName).revisionHistory.getMetadata(rev);
      for (MetadataScrubber scrubber : scrubbers) {
        rm = scrubber.scrub(rm);
      }
      rmBuilder.add(rm);
    }

    return RevisionMetadata.concatenate(rmBuilder.build(), fromRevision);
  }
}
