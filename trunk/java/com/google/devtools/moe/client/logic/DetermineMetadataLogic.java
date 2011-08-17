// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.MetadataScrubberConfig;
import com.google.devtools.moe.client.repositories.MetadataUsernameScrubber;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;

/**
 * Performs the logic of the DetermineMetadataDirective.
 *
 */
public class DetermineMetadataLogic {

  /**
   * Makes a RevisionMetadata for the given Revisions.
   *
   * @param context the context to evaluate in
   * @param revs the Revisions to include in the combined RevisionMetadata
   * @param fromRevision  when this method is used to get metadata for a migration, a Revision
   *                      describing the Revision in the from repository, otherwise null
   * @return  RevisionMetadata formed by combining the fields of the RevisionMetadata objects for
   *          each Revision in revs. Note that the description is formatted as follows, where rm[i]
   *          denotes the RevisionMetadata for the ith Revision of revs:
   *
   *          rm[0].description
   *            Change on rm[0].date by rm[0].author
   *          -------------
   *          rm[1].description
   *            Change on rm[1].date by rm[1].author
   *          -------------
   *          ...
   *          -------------
   *          rm[last].description
   *            Change on rm[last].date by rm[last].author
   *          -------------
   *          Created by MOE: https://code.google.com/p/moe-java
   *          MOE_MIGRATED_REVID=########
   *          [last two lines not included if fromRevision is null]
   */
  public static RevisionMetadata determine(ProjectContext context, List<Revision> revs,
                                           Revision fromRevision) {
    ImmutableList.Builder<String> idBuilder = ImmutableList.builder();
    ImmutableList.Builder<String> authorBuilder = ImmutableList.builder();
    ImmutableList.Builder<String> dateBuilder = ImmutableList.builder();
    ImmutableList.Builder<String> descBuilder = ImmutableList.builder();
    ImmutableList.Builder<Revision> parentBuilder = ImmutableList.builder();

    for (Revision rev : revs) {
      RevisionMetadata rm =
          context.repositories.get(rev.repositoryName).revisionHistory.getMetadata(rev);
      idBuilder.add(rm.id);
      authorBuilder.add(rm.author);
      dateBuilder.add(rm.date);
      descBuilder.add(String.format("%s\n\tChange on %s by %s",
                                    rm.description, rm.date, rm.author));
      parentBuilder.addAll(rm.parents);
    }

    if (fromRevision != null) {
      descBuilder.add("Created by MOE: https://code.google.com/p/moe-java\nMOE_MIGRATED_REVID=" +
                      context.repositories.get(fromRevision.repositoryName).revisionHistory.
                      findHighestRevision(fromRevision.revId).revId);
    }

    String newId = Joiner.on(", ").join(idBuilder.build());
    String newAuthor = Joiner.on(", ").join(authorBuilder.build());
    String newDate = Joiner.on(", ").join(dateBuilder.build());
    String newDesc = Joiner.on("\n-------------\n").join(descBuilder.build());
    ImmutableList<Revision> newParents = parentBuilder.build();

    return new RevisionMetadata(newId, newAuthor, newDate, newDesc, newParents);
  }

  /**
   * Get and scrub RevisionMetadata based on the given MetadataScrubberConfig.
   */
  public static RevisionMetadata determine(ProjectContext context, List<Revision> revs,
                                           MetadataScrubberConfig sc, Revision fromRevision) {
    RevisionMetadata unscrubbed = determine(context, revs, fromRevision);
    RevisionMetadata usersScrubbed = (new MetadataUsernameScrubber(
        sc.getUsernamesToScrub())).scrub(unscrubbed);
    if (sc.getScrubConfidentialWords()) {
      // Here is where you can use a MetadataScrubber that scrubs confidential words
      return usersScrubbed;
    } else {
      return usersScrubbed;
    }
  }
}
