// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.moe.client.logic;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.project.ProjectContext;
import com.google.devtools.moe.client.repositories.Revision;
import com.google.devtools.moe.client.repositories.RevisionMetadata;

import java.util.List;

/**
 * Performs the logic of the DetermineMetadataDirective.
 *
 */
public class DetermineMetadataLogic {


  /**
   * Logic for the DetermineMetadataDirective.
   *
   */
  public static RevisionMetadata determine(ProjectContext context, List<Revision> revs) {

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
      descBuilder.add(rm.description);
      parentBuilder.addAll(rm.parents);
    }

    String newId = Joiner.on(", ").join(idBuilder.build());
    String newAuthor = Joiner.on(", ").join(authorBuilder.build());
    String newDate = Joiner.on(", ").join(dateBuilder.build());
    String newDesc = Joiner.on("\n-------------\n").join(descBuilder.build());
    ImmutableList<Revision> newParents = parentBuilder.build();

    return new RevisionMetadata(newId, newAuthor, newDate, newDesc, newParents);
  }
}
